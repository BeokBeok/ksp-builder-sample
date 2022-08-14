package com.beok.ksp_builder_sample

import com.beok.annotations.AutoBuilder
import com.beok.annotations.BuilderProperty

@AutoBuilder(flexible = true)
data class Person(
    val name: String,
    @BuilderProperty val age: Int?,
    @BuilderProperty val email: String?,
    @BuilderProperty val contact: Pair<String, String>?
)
