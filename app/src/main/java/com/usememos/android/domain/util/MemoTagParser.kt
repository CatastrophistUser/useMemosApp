package com.usememos.android.domain.util

import com.usememos.android.domain.model.Memo
import com.usememos.android.domain.model.MemoTag

private val hashtagRegex = Regex("""(?<!\w)#([A-Za-z0-9_-]+)""")

fun extractTags(content: String): List<String> = hashtagRegex.findAll(content)
    .map { "#${it.groupValues[1].lowercase()}" }
    .distinct()
    .toList()

fun buildMemoTags(memos: List<Memo>): List<MemoTag> = memos
    .flatMap { extractTags(it.content) }
    .groupingBy { it }
    .eachCount()
    .map { (value, count) -> MemoTag(value = value, count = count) }
    .sortedWith(compareByDescending<MemoTag> { it.count }.thenBy { it.value })
