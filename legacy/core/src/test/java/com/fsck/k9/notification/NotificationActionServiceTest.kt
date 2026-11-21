package com.fsck.k9.notification

import android.app.Service
import android.content.ClipDescription
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.ClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationActionServiceTest : RobolectricTest() {
    private val account = LegacyAccountDto(ACCOUNT_UUID)
    private val preferences = mock<Preferences> {
        on { getAccount(ACCOUNT_UUID) } doReturn account
    }
    private val messagingController = mock<MessagingController>()
    private val interactionPreferences = mock<InteractionSettingsPreferenceManager>()

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { preferences }
                    single { messagingController }
                    single { interactionPreferences }
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
    fun `onStartCommand should copy verification code as sensitive clip and dismiss notification`() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        val messageReference = MessageReference(ACCOUNT_UUID, folderId = 1, uid = "uid")
        val testSubject = Robolectric.buildService(NotificationActionService::class.java).create().get()
        val intent = NotificationActionIntents.createCopyVerificationCodeIntent(context, messageReference, "482913")

        // Act
        val result = testSubject.onStartCommand(intent, 0, 1)

        // Assert
        assertThat(result).isEqualTo(Service.START_NOT_STICKY)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = requireNotNull(clipboard.primaryClip)
        assertThat(clip.getItemAt(0).text.toString()).isEqualTo("482913")
        assertThat(clip.description.extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE)).isTrue()
        verify(messagingController, timeout(1000)).cancelNotificationForMessage(account, messageReference)
    }
}
