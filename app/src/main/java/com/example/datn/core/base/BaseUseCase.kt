package com.example.datn.core.base

interface BaseUseCase<in Params, out Result> {
    operator fun invoke(params: Params): Result
}
