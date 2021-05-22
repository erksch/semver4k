package com.vdurmont.semver4j

import java.lang.RuntimeException

class SemverException : RuntimeException {
    constructor(msg: String?) : super(msg) {}
    constructor(msg: String?, t: Throwable?) : super(msg, t) {}
}