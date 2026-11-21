package com.fsck.k9.notification

import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.ClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric

class NotificationActionServiceTest : RobolectricTest() {
    private val accountManager = mock<LegacyAccountManager>()
    private val messagingController = mock<MessagingController>()
    private val interactionPreferences = mock<InteractionSettingsPreferenceManager>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { accountManager }
                    single { messagingController }
                    single { interactionPreferences }
                    single<Logger> { TestLogger() }
                    single { ClipboardManager(ApplicationProvider.getApplicationContext()) }
                    single<NotificationResourceProvider> { TestNotificationResourceProvider() }
                    single<CoroutineScope>(named("AppCoroutineScope")) { CoroutineScope(UnconfinedTestDispatcher()) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `onStartCommand should ignore an invalid account id without accessing accounts`() {
        // Arrange
        val testSubject = Robolectric.buildService(NotificationActionService::class.java).create().get()
        val intent = Intent().putExtra(EXTRA_ACCOUNT_UUID, "not-a-uuid")

        // Act
        val result = testSubject.onStartCommand(intent, 0, 1)

        // Assert
        assertThat(result).isEqualTo(android.app.Service.START_NOT_STICKY)
        verifyNoInteractions(accountManager, messagingController)
    }

    @Test
    fun `onStartCommand should copy verification code as sensitive clip and dismiss notification`() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        val messageReference = MessageReference(AccountIdFactory.create(), folderId = 1, uid = "uid")
        val testSubject = Robolectric.buildService(NotificationActionService::class.java).create().get()
        val intent = NotificationActionIntents.createCopyVerificationCodeIntent(context, messageReference, "482913")

        // Act
        testSubject.onStartCommand(intent, 0, 1)

        // Assert
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = requireNotNull(clipboard.primaryClip)
        assertThat(clip.getItemAt(0).text.toString()).isEqualTo("482913")
        assertThat(clip.description.extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE)).isTrue()
        verify(messagingController, timeout(1000)).cancelNotificationForMessage(
            messageReference.accountId,
            messageReference,
        )
    }
}
