package com.example.lendmark.utils

/**
 * LiveData 이벤트가 중복 실행되지 않도록 도와주는 유틸 클래스.
 * (ex. Toast, Navigation 등 "한 번만" 실행되어야 하는 경우)
 */
open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * 아직 처리되지 않았다면 content 반환 후 처리 완료로 표시
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * 이미 처리되었더라도 content를 그냥 확인하고 싶을 때 사용
     */
    fun peekContent(): T = content
}
