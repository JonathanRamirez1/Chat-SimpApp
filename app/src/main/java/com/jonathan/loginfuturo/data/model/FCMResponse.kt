package com.jonathan.loginfuturo.data.model

import java.util.ArrayList

class FCMResponse(private var multicast_id: Long,
                  private var success: Int,
                  private var failure: Int,
                  private var canonical_ids: Int,
                  results: ArrayList<Any>) {

    private var results = ArrayList<Any>()

    init {
        this.results = results
    }

    fun getMulticastId(): Long {
        return multicast_id
    }

    fun setMulticastId(multicast_id: Long) {
        this.multicast_id = multicast_id
    }

    fun getSuccess(): Int {
        return success
    }

    fun setSuccess(success: Int) {
        this.success = success
    }

    fun getFailure(): Int {
        return failure
    }

    fun setFailure(failure: Int) {
        this.failure = failure
    }

    fun getCanonicalId(): Int {
        return canonical_ids
    }

    fun setCanonicalId(canonical_ids: Int) {
        this.canonical_ids = canonical_ids
    }

    fun getResult(): ArrayList<Any> {
        return results
    }

    fun setResult(results: ArrayList<Any>) {
        this.results = results
    }
}