package com.mistyislet.app.core.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * NFC 读卡器 — 用于自助绑定 DESFire EV3 实体卡。
 * 读取卡片 UID 后发送给后端进行绑定。
 */
class NFCReader {

    private val _tagEvents = Channel<NFCTagEvent>(Channel.BUFFERED)
    val tagEvents: Flow<NFCTagEvent> = _tagEvents.receiveAsFlow()

    data class NFCTagEvent(
        val uid: String,
        val techList: List<String>,
    )

    private var nfcAdapter: NfcAdapter? = null

    fun isAvailable(activity: Activity): Boolean {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        return nfcAdapter != null
    }

    fun isEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * 启用 Reader 模式，持续监听 NFC 标签。
     * 需要在 Activity onResume 中调用。
     */
    fun enableReaderMode(activity: Activity) {
        val adapter = nfcAdapter ?: NfcAdapter.getDefaultAdapter(activity) ?: return
        nfcAdapter = adapter

        val callback = NfcAdapter.ReaderCallback { tag ->
            val uid = tag.id.toHexString()
            val techs = tag.techList.map { it.substringAfterLast('.') }
            _tagEvents.trySend(NFCTagEvent(uid = uid, techList = techs))
        }

        adapter.enableReaderMode(
            activity,
            callback,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            Bundle.EMPTY,
        )
    }

    /**
     * 禁用 Reader 模式。
     * 需要在 Activity onPause 中调用。
     */
    fun disableReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}
