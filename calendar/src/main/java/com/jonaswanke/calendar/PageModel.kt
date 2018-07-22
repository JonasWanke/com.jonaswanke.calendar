package com.jonaswanke.calendar


import android.view.View
import android.view.ViewGroup

class PageModel<T>(val wrapper: ViewGroup, val view: View, var indicator: T?)
