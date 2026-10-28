/**
 * MINISTRY OF HEALTH AND CHILD CARE OF ZIMBABWE (MoHCC)
 * CHILD IMMUNISATION TRACKING & EHR INTEROPERABILITY SYSTEM (CITS)
 *
 * Cloud Functions for Firebase (Node.js & Firebase Admin SDK)
 * Target Firebase Project: cits-4d830
 * Target App Package: com.matshelela.childimmunisationtrackingsystemappv1
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

// Recognized Ministry Role Types
const ROLES = {
  MOH_ADMIN: "MOH_ADMIN",
  FACILITY_ADMIN: "FACILITY_ADMIN",
  DOCTOR_NURSE: "DOCTOR_NURSE",
  PARENT: "PARENT",
};

/**
 * ------------------------------------------------------------------------
 * 1. ROLE-BASED ACCESS CONTROL (CUSTOM CLAIMS ASSIGNMENT)
 * ------------------------------------------------------------------------
 * Assigns cryptographic custom claims to Firebase Authentication tokens.
 * Custom claims cannot be edited or forged by mobile clients.
 *
 * Security: Only authenticated users with `MOH_ADMIN` claim can assign roles.
 */
exports.setUserRole = functions.https.onCall(async (data, context) => {
  // 1. Abuse Prevention: Enforce App Check verification
  if (process.env.NODE_ENV === "production" && !context.app) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Request must originate from a verified Play Integrity app (App Check failed)."
    );
  }

  // 2. Authentication Verification
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication is required to set administrative claims."
    );
  }

  // 3. Authorization: Only MOH Admin can assign roles (bootstrapping can check root admin email)
  const callerClaims = context.auth.token || {};
  const isCallerMohAdmin = callerClaims.role === ROLES.MOH_ADMIN;
  const isBootstrapAdmin = context.auth.token.email === "admin.cits@mohcc.gov.zw";

  if (!isCallerMohAdmin && !isBootstrapAdmin) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only MoHCC System Administrators are authorized to provision user roles."
    );
  }

  const { targetUid, role, facilityId } = data;

  if (!targetUid || !role || !Object.values(ROLES).includes(role)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `Invalid role parameter. Permitted roles: ${Object.values(ROLES).join(", ")}`
    );
  }

  try {
    // Set custom claims on Firebase Auth
    await admin.auth().setCustomUserClaims(targetUid, {
      role: role,
      facilityId: facilityId || null,
      updatedAt: Date.now(),
    });

    // Write audit log entry
    await db.collection("audit_role_assignments").add({
      targetUid,
      assignedRole: role,
      assignedBy: context.auth.uid,
      facilityId: facilityId || null,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      status: "success",
      message: `Role ${role} successfully granted to user ${targetUid}.`,
    };
  } catch (error) {
    console.error("Error setting custom user claims:", error);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * ------------------------------------------------------------------------
 * 2. SECURE CODE GENERATION (Facility Admin -> Parent Pairing)
 * ------------------------------------------------------------------------
 * Generates an 8-character cryptographically secure pairing code (e.g., ZW-LINK-4891).
 *
 * Security: Only FACILITY_ADMIN can invoke code generation.
 */
exports.generatePairingCode = functions.https.onCall(async (data, context) => {
  // 1. Enforce App Check
  if (process.env.NODE_ENV === "production" && !context.app) {
    throw new functions.https.HttpsError("failed-precondition", "App Check verification failed.");
  }

  // 2. Auth check
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  // 3. Role check: Facility Admin
  const role = context.auth.token.role;
  if (role !== ROLES.FACILITY_ADMIN && role !== ROLES.MOH_ADMIN) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only registered Facility Administrators can generate child pairing codes."
    );
  }

  const { childId, clinicId } = data;
  if (!childId) {
    throw new functions.https.HttpsError("invalid-argument", "childId is required.");
  }

  // Generate 4 cryptographically secure random digits
  const randomSuffix = crypto.randomInt(1000, 9999);
  const pairingCode = `ZW-LINK-${randomSuffix}`;

  // Expiration: 72 hours from generation
  const expiresAt = Date.now() + 72 * 60 * 60 * 1000;

  const codeDocRef = db.collection("pairing_codes").doc(pairingCode);

  await codeDocRef.set({
    pairingCode,
    childId: String(childId),
    clinicId: clinicId || null,
    generatedBy: context.auth.uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt: expiresAt,
    isRedeemed: false,
    redeemedByParentUid: null,
  });

  // Update child profile record with access code
  const childDocRef = db.collection("children").doc(String(childId));
  await childDocRef.set(
    {
      accessCode: pairingCode,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  return {
    status: "success",
    pairingCode,
    expiresAt,
  };
});

/**
 * ------------------------------------------------------------------------
 * 3. SECURE CODE REDEMPTION & PAIRING (Parent Portal Entry)
 * ------------------------------------------------------------------------
 * Validates the temporary pairing code server-side and establishes the
 * parent-child link in Firestore without granting client-side write access
 * to medical demographics.
 */
exports.linkChildWithCode = functions.https.onCall(async (data, context) => {
  // 1. Enforce App Check
  if (process.env.NODE_ENV === "production" && !context.app) {
    throw new functions.https.HttpsError("failed-precondition", "App Check verification failed.");
  }

  // 2. Auth check
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Parent must be signed in with Google Auth.");
  }

  const parentUid = context.auth.uid;
  const rawCode = (data.accessCode || "").trim().toUpperCase();

  if (!rawCode) {
    throw new functions.https.HttpsError("invalid-argument", "Please enter the pairing access code.");
  }

  const codeDocRef = db.collection("pairing_codes").doc(rawCode);

  return await db.runTransaction(async (transaction) => {
    const codeDoc = await transaction.get(codeDocRef);

    if (!codeDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Invalid pairing access code. Please check with your clinic.");
    }

    const codeData = codeDoc.data();

    if (codeData.isRedeemed) {
      throw new functions.https.HttpsError("already-exists", "This pairing code has already been redeemed.");
    }

    if (codeData.expiresAt && Date.now() > codeData.expiresAt) {
      throw new functions.https.HttpsError("deadline-exceeded", "This pairing code has expired. Please request a new code from your clinic.");
    }

    const childId = String(codeData.childId);
    const childDocRef = db.collection("children").doc(childId);
    const childDoc = await transaction.get(childDocRef);

    if (!childDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Target child record not found in national registry.");
    }

    // 1. Mark code as redeemed
    transaction.update(codeDocRef, {
      isRedeemed: true,
      redeemedByParentUid: parentUid,
      redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // 2. Mark child profile as linked to this parent
    transaction.update(childDocRef, {
      isLinkedToParent: true,
      parentUid: parentUid,
      linkedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // 3. Create persistent child access grant
    const grantId = `${parentUid}_${childId}`;
    const grantDocRef = db.collection("child_access_grants").doc(grantId);
    transaction.set(grantDocRef, {
      parentUid: parentUid,
      childId: childId,
      grantedAt: admin.firestore.FieldValue.serverTimestamp(),
      revoked: false,
    });

    const childData = childDoc.data();
    return {
      status: "success",
      message: `Successfully linked to ${childData.fullName || "Child"} (ID: ${childId}).`,
      childId: childId,
      fullName: childData.fullName || "",
      assignedDoctorName: childData.assignedDoctorName || "Assigned Pediatrician",
      healthChallenges: childData.healthChallenges || "None recorded",
    };
  });
});
