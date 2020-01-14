package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Bundle
import java.lang.IllegalArgumentException

private val VERSION_KEY = "version"
private val CLASS_KEY = "class"

private val VERSION = 1

// A few methods to allow us to just call `put` and let operator overloading take it from there.
internal fun Bundle.put(key: String, value: String?) = putString(key, value)
internal fun Bundle.put(key: String, value: Int) = putInt(key, value)
internal fun Bundle.put(key: String, value: List<String>) =
        putStringArray(key, value.toTypedArray())
internal fun Bundle.put(key: String, value: Array<Bundle>) = putParcelableArray(key, value)
internal fun Bundle.put(key: String, value: Bundle) = putBundle(key, value)

/** Helper method that maps the input list into a ParcelableArray using `bundlify`. */
internal fun <T> listToParcelableArray(list: MutableList<T>, bundlify: (T) -> Bundle) =
        list.map(bundlify).toTypedArray()

/** Creates a Bundle with the given class name and the version code. */
internal fun makeBundle(className: String, modify: Bundle.() -> Unit): Bundle {
    val bundle = Bundle()
    bundle.putString(CLASS_KEY, className)
    bundle.putInt(VERSION_KEY, VERSION)
    bundle.modify()
    return bundle
}

/**
 * Checks that the given Bundle contains the expected class name, throws IllegalArgumentException if
 * not.
 */
internal fun checkBundle(bundle: Bundle, expectedClass: String) {
    val providedClass = bundle.getString(CLASS_KEY)
    if (providedClass != expectedClass) {
        throw IllegalArgumentException("Provided class ($providedClass) " +
                "does not match expected class ($expectedClass)")
    }
}