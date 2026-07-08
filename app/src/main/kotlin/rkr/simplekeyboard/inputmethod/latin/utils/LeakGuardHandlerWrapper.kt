package rkr.simplekeyboard.inputmethod.latin.utils

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

open class LeakGuardHandlerWrapper<T>(ownerInstance: T, looper: Looper? = Looper.myLooper()) :
    Handler(looper!!) {
    private val mOwnerInstanceRef = WeakReference(ownerInstance)

    val ownerInstance: T?
        get() = mOwnerInstanceRef.get()
}
