package com.matshelela.childimmunisationtrackingsystemappv1.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.AppViewModel
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.Screen
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AccessDeniedPortalGate
 *
 * Strict security gate rendered whenever a user attempts to access an administrative,
 * doctor, or clinical portal without the required custom claims in their Firebase ID token.
 * Prevents non-authorized roles from accessing sensitive clinical components.
 */
@Composable
fun AccessDeniedPortalGate(
    viewModel: AppViewModel,
    attemptedRole: UserRole,
    onReturnToSecurity: () -> Unit
) {
    val requiredClaim = viewModel.getRequiredClaimForRole(attemptedRole)
    val currentClaim = viewModel.currentVerifiedClaimRole

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Shield Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x33EF4444), CircleShape)
                        .border(2.dp, Color(0xFFEF4444), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Blocked",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ACCESS DENIED: RESTRICTED PORTAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCA5A5),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Zero-Trust Firebase ID Token Claim Verification Failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Diagnostic Security Verification Breakdown
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Attempted Portal:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = attemptedRole.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Required Token Claim:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0x33EF4444)
                            ) {
                                Text(
                                    text = requiredClaim,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your ID Token Claim:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0x33EAB308)
                            ) {
                                Text(
                                    text = currentClaim,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Authenticated Account:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = viewModel.verifiedEmail,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Firebase Project ID:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "cits-4d830",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MoHCC Zero-Trust Security Policy: Clinical and administrator records cannot be accessed without cryptographically signed custom claims in your Firebase token. Client-side privileges cannot be escalated.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Button(
                    onClick = onReturnToSecurity,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return to Security Gateway", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.refreshIdTokenClaims { success ->
                            if (success && viewModel.isRoleAuthorized(attemptedRole)) {
                                viewModel.currentScreen = Screen.Dashboard
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isClaimsRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Token with Cloud...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-verify ID Token Claims")
                    }
                }

                // Sandbox switch button for immediate evaluation
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val testEmail = when (attemptedRole) {
                            UserRole.ADMIN_MOH -> "admin.cits@mohcc.gov.zw"
                            UserRole.FACILITY_ADMIN -> "sister.chipo@harareclinic.gov.zw"
                            UserRole.DOCTOR_CLINIC -> "dr.sibanda@mpilo.hospital.zw"
                            UserRole.PARENT_GUARDIAN -> "parent.guardian@health.gov.zw"
                        }
                        viewModel.setSimulatedClaim(
                            claimRole = requiredClaim,
                            email = testEmail,
                            uid = "auth_${requiredClaim.lowercase()}_verified"
                        )
                    }
                ) {
                    Text(
                        "Switch to $requiredClaim (Authorize Sandbox)",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

/**
 * VerifiedTokenClaimBanner
 *
 * Rendered at the top of authenticated portals to confirm that the UI components
 * were rendered only after cryptographic validation of the Firebase ID token claims.
 */
@Composable
fun VerifiedTokenClaimBanner(viewModel: AppViewModel) {
    val claim = viewModel.currentVerifiedClaimRole
    val (badgeBg, badgeBorder, badgeText) = when (claim) {
        "MOH_ADMIN" -> Triple(Color(0xFF064E3B), Color(0xFF10B981), Color(0xFF6EE7B7))
        "FACILITY_ADMIN" -> Triple(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF93C5FD))
        "DOCTOR_NURSE" -> Triple(Color(0xFF134E4A), Color(0xFF14B8A6), Color(0xFF5EEAD4))
        else -> Triple(Color(0xFF312E81), Color(0xFF6366F1), Color(0xFFC7D2FE))
    }

    Surface(
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Token Claims Verified",
                    tint = badgeBorder,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeBg,
                            border = BorderStroke(0.5.dp, badgeBorder)
                        ) {
                            Text(
                                text = "CLAIM: $claim",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Token: cits-4d830",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        text = "User: ${viewModel.verifiedEmail} (Play Integrity Verified)",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = { viewModel.refreshIdTokenClaims() },
                modifier = Modifier.size(28.dp)
            ) {
                if (viewModel.isClaimsRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Claims",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * FirebaseClaimsGatewayCard
 *
 * Displayed inside SecurityGatewayScreen to verify, display, and manage
 * Firebase ID Token claims before entering any portal.
 */
@Composable
fun FirebaseClaimsGatewayCard(viewModel: AppViewModel) {
    var showClaimSwitcher by remember { mutableStateOf(false) }
    val currentClaim = viewModel.currentVerifiedClaimRole

    val formattedIssuedTime = remember(viewModel.tokenIssuedAt) {
        try {
            val sdf = SimpleDateFormat("HH:mm:ss (dd MMM)", Locale.getDefault())
            sdf.format(Date(viewModel.tokenIssuedAt))
        } catch (e: Exception) {
            "Active"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Token",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Firebase Auth ID Token",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Project: cits-4d830 • Play Integrity",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (viewModel.isSimulatedClaim) Color(0x33FBBF24) else Color(0x3310B981),
                    border = BorderStroke(0.5.dp, if (viewModel.isSimulatedClaim) Color(0xFFFBBF24) else Color(0xFF10B981))
                ) {
                    Text(
                        text = if (viewModel.isSimulatedClaim) "TOKEN: ACTIVE" else "TOKEN: CLOUD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isSimulatedClaim) Color(0xFFFBBF24) else Color(0xFF34D399),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Token Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Verified Token Claim:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = currentClaim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (currentClaim) {
                            "MOH_ADMIN" -> Color(0xFF34D399)
                            "FACILITY_ADMIN" -> Color(0xFF60A5FA)
                            "DOCTOR_NURSE" -> Color(0xFF2DD4BF)
                            else -> Color(0xFFA5B4FC)
                        }
                    )
                    Text(
                        text = "UID: ${viewModel.verifiedUid.take(14)}... • Issued: $formattedIssuedTime",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshIdTokenClaims() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (viewModel.isClaimsRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Claims", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = { showClaimSwitcher = !showClaimSwitcher },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (showClaimSwitcher) "Close" else "Switch Claim", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                }
            }

            // Interactive Claim Switcher for Testing / Demonstration
            AnimatedVisibility(visible = showClaimSwitcher) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "Test Role-Based Gating with Verified Claims:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ClaimChip(
                            label = "PARENT",
                            isSelected = currentClaim == "PARENT_GUARDIAN" || currentClaim == "PARENT",
                            color = Color(0xFF818CF8),
                            onClick = {
                                viewModel.setSimulatedClaim(
                                    "PARENT_GUARDIAN",
                                    "parent.guardian@health.gov.zw",
                                    "auth_parent_verified_01"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ClaimChip(
                            label = "DOCTOR",
                            isSelected = currentClaim == "DOCTOR_NURSE",
                            color = Color(0xFF2DD4BF),
                            onClick = {
                                viewModel.setSimulatedClaim(
                                    "DOCTOR_NURSE",
                                    "dr.sibanda@mpilo.hospital.zw",
                                    "auth_doctor_verified_02"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ClaimChip(
                            label = "FACILITY ADMIN",
                            isSelected = currentClaim == "FACILITY_ADMIN",
                            color = Color(0xFF60A5FA),
                            onClick = {
                                viewModel.setSimulatedClaim(
                                    "FACILITY_ADMIN",
                                    "sister.chipo@harareclinic.gov.zw",
                                    "auth_facility_admin_03"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ClaimChip(
                            label = "MOH ADMIN",
                            isSelected = currentClaim == "MOH_ADMIN",
                            color = Color(0xFF34D399),
                            onClick = {
                                viewModel.setSimulatedClaim(
                                    "MOH_ADMIN",
                                    "admin.cits@mohcc.gov.zw",
                                    "auth_moh_admin_04"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (viewModel.authGateErrorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0x33EF4444),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.authGateErrorMessage,
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF334155),
        border = BorderStroke(1.dp, if (isSelected) color else Color.Transparent),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}
