package com.rainyseason.cj.common

import org.junit.Assert
import org.junit.Test

class PageListUtilTest {

    @Test
    fun `max page 0`() {
        val list = createList(0)
        val result = list.maxPage(DEFAULT_PER_PAGE)
        Assert.assertEquals(0, result)
    }

    @Test
    fun `max page 1`() {
        val list = createList(50)
        val result = list.maxPage(DEFAULT_PER_PAGE)
        Assert.assertEquals(0, result)
    }

    @Test
    fun `max page 2`() {
        val list = createList(100)
        val result = list.maxPage(DEFAULT_PER_PAGE)
        Assert.assertEquals(0, result)
    }

    @Test
    fun `max page 3`() {
        val list = createList(101)
        val result = list.maxPage(DEFAULT_PER_PAGE)
        Assert.assertEquals(1, result)
    }

    @Test
    fun `max page 4`() {
        val list = createList(200)
        val result = list.maxPage(DEFAULT_PER_PAGE)
        Assert.assertEquals(1, result)
    }

    @Test
    fun `max page 5`() {
        val list = createList(201)
        val result = list.maxPage(DEFAULT_PER_PAGE)
        Assert.assertEquals(2, result)
    }

    @Test
    fun `50 - page 0`() {
        val list = createList(50)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 49,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `50 - page 1`() {
        val list = createList(50)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 49,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `100 - page 0`() {
        val list = createList(100)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `100 - page 1`() {
        val list = createList(100)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `100 - page 2`() {
        val list = createList(100)
        val result = createSelectPagedListParam(
            list = list,
            page = 2,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `150 - page 0`() {
        // [    v     ]
        // -1   0     1
        // -100 0   100
        val list = createList(150)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `150 - page 1`() {
        // [    v    ]
        // 0    1    2
        // 0   100 200
        val list = createList(150)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 149,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `150 - page 2`() {
        // [    v    ]
        // 0    1    2
        // 0   100 200
        val list = createList(150)
        val result = createSelectPagedListParam(
            list = list,
            page = 2,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 149,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `200 - page 0`() {
        // [    v     ]
        // -1   0     1
        // -100 0   100
        val list = createList(200)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `200 - page 1`() {
        // [    v    ]
        // 0    1    2
        // 0   100 200
        val list = createList(200)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 199,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `200 - page 2`() {
        // [    v    ]
        // 0    1    2
        // 0   100 200
        val list = createList(200)
        val result = createSelectPagedListParam(
            list = list,
            page = 2,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 199,
            showStartLoading = false,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `250 - page 0`() {
        // [    v    ]
        // -1   0    1    2
        // -100 0   100 200 300
        val list = createList(250)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `250 - page 1`() {
        // [    v    ]
        // 0    1    2
        // 0   100 200 300
        val list = createList(250)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 199,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `250 - page 2`() {
        //      [    v    ]
        // 0    1    2
        // 0   100 200 300
        val list = createList(250)
        val result = createSelectPagedListParam(
            list = list,
            page = 2,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 100,
            toIndex = 249,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `250 - page 3`() {
        //      [    v    ]
        // 0    1    2
        // 0   100 200 300
        val list = createList(250)
        val result = createSelectPagedListParam(
            list = list,
            page = 3,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 100,
            toIndex = 249,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `300 - page 0`() {
        // [    v    ]
        // -1   0    1    2
        // -100 0   100 200 300
        val list = createList(300)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `300 - page 1`() {
        //      [    v    ]
        // -1   0    1    2   3
        // -100 0   100 200 300
        val list = createList(300)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 199,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `300 - page 2`() {
        //          [    v    ]
        // -1   0    1    2   3
        // -100 0   100 200 300
        val list = createList(300)
        val result = createSelectPagedListParam(
            list = list,
            page = 2,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 100,
            toIndex = 299,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `300 - page 3`() {
        //          [    v    ]
        // -1   0    1    2   3
        // -100 0   100 200 300
        val list = createList(300)
        val result = createSelectPagedListParam(
            list = list,
            page = 3,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 100,
            toIndex = 299,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `350 - page 0`() {
        // [    v    ]
        // -1   0    1    2   3
        // -100 0   100 200 300
        val list = createList(350)
        val result = createSelectPagedListParam(
            list = list,
            page = 0,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 99,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `350 - page 1`() {
        //      [    v    ]
        // -1   0    1    2   3   4
        // -100 0   100 200 300 400
        val list = createList(350)
        val result = createSelectPagedListParam(
            list = list,
            page = 1,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 0,
            toIndex = 199,
            showStartLoading = false,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `350 - page 2`() {
        //          [    v    ]
        // -1   0    1    2   3   4
        // -100 0   100 200 300 400
        val list = createList(350)
        val result = createSelectPagedListParam(
            list = list,
            page = 2,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 100,
            toIndex = 299,
            showStartLoading = true,
            showEndLoading = true,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `350 - page 3`() {
        //               [    v    ]
        // -1   0    1    2   3   4
        // -100 0   100 200 300 400
        val list = createList(350)
        val result = createSelectPagedListParam(
            list = list,
            page = 3,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 200,
            toIndex = 349,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `350 - page 4`() {
        //               [    v    ]
        // -1   0    1    2   3   4
        // -100 0   100 200 300 400
        val list = createList(350)
        val result = createSelectPagedListParam(
            list = list,
            page = 4,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 200,
            toIndex = 349,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `400 - page 3`() {
        //               [    v    ]
        // -1   0    1    2   3   4
        // -100 0   100 200 300 400
        val list = createList(400)
        val result = createSelectPagedListParam(
            list = list,
            page = 3,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 200,
            toIndex = 399,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `400 - page 4`() {
        //               [    v    ]
        // -1   0    1    2   3   4
        // -100 0   100 200 300 400
        val list = createList(400)
        val result = createSelectPagedListParam(
            list = list,
            page = 4,
            perPage = DEFAULT_PER_PAGE
        )
        val expected = SelectListParams(
            fromIndex = 200,
            toIndex = 399,
            showStartLoading = true,
            showEndLoading = false,
        )
        Assert.assertEquals(expected, result)
    }

    private fun createList(numberOfItems: Int): List<Int> {
        val result = mutableListOf<Int>()
        repeat(numberOfItems) {
            result.add(it)
        }
        return result
    }
}
