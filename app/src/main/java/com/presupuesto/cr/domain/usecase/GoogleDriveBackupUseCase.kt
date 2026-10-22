package com.presupuesto.cr.domain.usecase

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class GoogleDriveBackupUseCase(
    private val context: Context
) {
    companion object {
        const val TARGET_ACCOUNT_EMAIL = "tavo03079999@gmail.com"
        private const val DB_FILE_NAME = "presupuesto_cr_encrypted.db"
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .setAccountName(TARGET_ACCOUNT_EMAIL)
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && account.email.equals(TARGET_ACCOUNT_EMAIL, ignoreCase = true)) {
            account
        } else {
            account
        }
    }

    suspend fun uploadDatabaseBackup(account: GoogleSignInAccount): Result<String> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
            }

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Presupuesto Inteligente CR").build()

            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(IllegalStateException("No se encontró el archivo de base de datos local"))
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val metadata = DriveFile().apply {
                name = "Backup_PresupuestoCR_${timeStamp}.db"
                description = "Respaldo seguro cifrado de Presupuesto CR para $TARGET_ACCOUNT_EMAIL"
            }

            val mediaContent = com.google.api.client.http.FileContent("application/x-sqlite3", dbFile)
            val uploadedFile = driveService.files().create(metadata, mediaContent)
                .setFields("id, name, createdTime")
                .execute()

            Result.success("Respaldo exitoso en Google Drive: ${uploadedFile.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDatabaseBackup(account: GoogleSignInAccount, driveFileId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
            }

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Presupuesto Inteligente CR").build()

            val destinationDb = context.getDatabasePath(DB_FILE_NAME)
            FileOutputStream(destinationDb).use { output ->
                driveService.files().get(driveFileId).executeMediaAndDownloadTo(output)
            }

            Result.success("Base de datos restaurada correctamente desde Google Drive")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
