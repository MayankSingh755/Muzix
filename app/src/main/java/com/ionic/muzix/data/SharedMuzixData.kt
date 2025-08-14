package com.ionic.muzix.data


/**
 * Singleton to share muzix data between activities
 * This helps when expanding from mini player without passing all data through intent
 */
object SharedMuzixData {
    private var _muzixList: List<Muzix> = emptyList()
    private var _currentIndex: Int = 0

    var muzixList: List<Muzix>
        get() = _muzixList
        set(value) {
            _muzixList = value
        }

    var currentIndex: Int
        get() = _currentIndex
        set(value) {
            _currentIndex = value
        }

    fun setData(list: List<Muzix>, index: Int) {
        _muzixList = list
        _currentIndex = index
    }

    fun hasData(): Boolean = _muzixList.isNotEmpty()
}