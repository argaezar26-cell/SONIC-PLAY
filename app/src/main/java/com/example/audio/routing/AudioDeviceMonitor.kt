package com.example.audio.routing

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.data.model.MixerVerificationStatus
import com.example.data.model.OutputDeviceSummary
import com.example.data.model.OutputRouteCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioRoutingState(
    val outputDevices: List<OutputDeviceSummary> = emptyList(),
    val activeDevice: OutputDeviceSummary? = null,
    val lastDisconnectEvent: String? = null
)

class AudioDeviceMonitor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _routingState = MutableStateFlow(AudioRoutingState())
    val routingState: StateFlow<AudioRoutingState> = _routingState.asStateFlow()

    private var onUsbOrHeadsetRemoved: ((String) -> Unit)? = null

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val removedUsbOrBt = removedDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            refreshDevices()
            if (removedUsbOrBt != null) {
                val label = removedUsbOrBt.productName?.toString()?.ifBlank { null }
                    ?: categorizeDevice(removedUsbOrBt.type).displayName
                _routingState.value = _routingState.value.copy(
                    lastDisconnectEvent = "$label disconnected — playback paused safely"
                )
                onUsbOrHeadsetRemoved?.invoke(label)
            }
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        refreshDevices()
    }

    fun setDisconnectListener(listener: (String) -> Unit) {
        onUsbOrHeadsetRemoved = listener
    }

    fun clearDisconnectBanner() {
        _routingState.value = _routingState.value.copy(lastDisconnectEvent = null)
    }

    fun refreshDevices() {
        val rawDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val summaries = rawDevices
            .filter {
                it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE &&
                    it.type != AudioDeviceInfo.TYPE_TELEPHONY
            }
            .map { info -> buildSummary(info) }
            .distinctBy { "${it.deviceType}_${it.productName}" }

        // Select most likely active route in Android priority order:
        // External USB DAC / USB Headset -> Wired Headphones -> Bluetooth A2DP -> Built-in Speakers
        val prioritized = summaries.firstOrNull {
            it.routeCategory == OutputRouteCategory.EXTERNAL_USB_DAC ||
                it.routeCategory == OutputRouteCategory.USB_TYPE_C_ADAPTER
        } ?: summaries.firstOrNull {
            it.routeCategory == OutputRouteCategory.WIRED_HEADPHONES
        } ?: summaries.firstOrNull {
            it.routeCategory == OutputRouteCategory.BLUETOOTH
        } ?: summaries.firstOrNull {
            it.routeCategory == OutputRouteCategory.BUILT_IN_SPEAKERS
        } ?: summaries.firstOrNull()

        _routingState.value = _routingState.value.copy(
            outputDevices = summaries,
            activeDevice = prioritized
        )
    }

    fun configureBitPerfectMixerIfSupported(preferredSampleRateHz: Int): MixerVerificationStatus {
        val active = _routingState.value.activeDevice ?: return MixerVerificationStatus.UNKNOWN
        if (Build.VERSION.SDK_INT < 34) {
            return if (active.supportedSampleRates.isEmpty() || preferredSampleRateHz in active.supportedSampleRates) {
                MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
            } else {
                MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
            }
        }

        val rawDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val deviceInfo = rawDevices.firstOrNull { it.id == active.id } ?: return MixerVerificationStatus.UNKNOWN
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val supportedAttrs = audioManager.getSupportedMixerAttributes(deviceInfo)
            val bitPerfectCandidate = supportedAttrs.firstOrNull {
                it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                    (preferredSampleRateHz <= 0 || it.format.sampleRate == preferredSampleRateHz)
            } ?: supportedAttrs.firstOrNull {
                it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
            }

            if (bitPerfectCandidate != null) {
                val applied = audioManager.setPreferredMixerAttributes(attrs, deviceInfo, bitPerfectCandidate)
                refreshDevices()
                if (applied) MixerVerificationStatus.VERIFIED_ANDROID_MIXER
                else MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
            } else {
                MixerVerificationStatus.UNSUPPORTED
            }
        } catch (_: Exception) {
            MixerVerificationStatus.UNKNOWN
        }
    }

    fun clearPreferredMixerAttributes() {
        if (Build.VERSION.SDK_INT < 34) return
        val active = _routingState.value.activeDevice ?: return
        val deviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == active.id } ?: return
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioManager.clearPreferredMixerAttributes(attrs, deviceInfo)
            refreshDevices()
        } catch (_: Exception) {
        }
    }

    private fun buildSummary(info: AudioDeviceInfo): OutputDeviceSummary {
        val category = categorizeDevice(info.type)
        val rawName = info.productName?.toString()?.trim().orEmpty()
        val displayName = when {
            category == OutputRouteCategory.BUILT_IN_SPEAKERS && (rawName.isBlank() || rawName.equals(Build.MODEL, true)) ->
                "Dual Stereo Speakers (JBL / System)"
            rawName.isNotBlank() -> rawName
            else -> category.displayName
        }

        val rates = info.sampleRates.toList().sorted()
        val channels = info.channelCounts.toList().sorted()
        val encodings = info.encodings.map { encodingLabel(it) }.distinct()

        var mixerStatus = MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
        var bitPerfectSupported = false
        var activeMixerSummary: String? = null

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val supportedMixers = audioManager.getSupportedMixerAttributes(info)
                bitPerfectSupported = supportedMixers.any {
                    it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                }
                val currentPreferred = audioManager.getPreferredMixerAttributes(attrs, info)
                if (currentPreferred != null) {
                    activeMixerSummary = "${currentPreferred.format.sampleRate} Hz • ${encodingLabel(currentPreferred.format.encoding)}"
                    mixerStatus = if (currentPreferred.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT) {
                        MixerVerificationStatus.VERIFIED_ANDROID_MIXER
                    } else {
                        MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
                    }
                } else {
                    mixerStatus = if (bitPerfectSupported) {
                        MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
                    } else if (category == OutputRouteCategory.EXTERNAL_USB_DAC) {
                        MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
                    } else {
                        MixerVerificationStatus.UNSUPPORTED
                    }
                }
            } catch (_: Exception) {
                mixerStatus = MixerVerificationStatus.UNKNOWN
            }
        } else {
            mixerStatus = if (rates.isNotEmpty() || encodings.isNotEmpty()) {
                MixerVerificationStatus.COMPATIBLE_NOT_VERIFIED
            } else {
                MixerVerificationStatus.UNKNOWN
            }
        }

        val btHint = if (category == OutputRouteCategory.BLUETOOTH) {
            "Route active (codec determined by Android BT stack)"
        } else null

        return OutputDeviceSummary(
            id = info.id,
            productName = displayName,
            routeCategory = category,
            deviceType = info.type,
            supportedSampleRates = rates,
            supportedEncodings = encodings,
            supportedChannelCounts = channels,
            bluetoothCodecHint = btHint,
            mixerStatus = mixerStatus,
            bitPerfectSupportedByMixer = bitPerfectSupported,
            activeMixerFormatSummary = activeMixerSummary
        )
    }

    private fun categorizeDevice(type: Int): OutputRouteCategory {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> OutputRouteCategory.BUILT_IN_SPEAKERS
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> OutputRouteCategory.EXTERNAL_USB_DAC
            AudioDeviceInfo.TYPE_USB_HEADSET -> OutputRouteCategory.USB_TYPE_C_ADAPTER
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> OutputRouteCategory.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> OutputRouteCategory.BLUETOOTH
            else -> OutputRouteCategory.UNKNOWN
        }
    }

    private fun encodingLabel(encoding: Int): String {
        return when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> "PCM 16-bit"
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24-bit"
            AudioFormat.ENCODING_PCM_32BIT -> "PCM 32-bit"
            AudioFormat.ENCODING_PCM_FLOAT -> "PCM Float"
            AudioFormat.ENCODING_PCM_8BIT -> "PCM 8-bit"
            else -> "Encoding #$encoding"
        }
    }

    fun release() {
        runCatching { audioManager.unregisterAudioDeviceCallback(callback) }
    }
}
