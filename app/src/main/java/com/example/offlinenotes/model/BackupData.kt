package com.example.offlinenotes.model

import java.io.Serializable

data class BackupData(
    val version: Int = 4, // Bumped to V4 for Vault support
    val folders: List<Folder> = emptyList(),
    val notes: List<Note> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    // V4 Security Metadata
    val vaultSalt: String? = null,
    val vaultVerificationToken: String? = null,
    val vaultVerificationIv: String? = null
) : Serializable