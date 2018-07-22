package com.jonaswanke.calendar


import android.view.View
import android.view.ViewGroup

class PageModel<T>(val wrapper: ViewGroup, var view: View, var indicator: T?)
