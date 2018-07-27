package com.jonaswanke.calendar


import android.view.View
import android.view.ViewGroup

class PageModel<T, V : View>(val wrapper: ViewGroup, var view: V, var indicator: T?)
