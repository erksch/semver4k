package com.vdurmont.semver4j

class SemverException : RuntimeException {
    constructor(msg: String?) : super(msg) {}
    constructor(msg: String?, t: Throwable?) : super(msg, t) {}
}