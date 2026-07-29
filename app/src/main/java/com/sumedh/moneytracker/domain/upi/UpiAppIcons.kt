package com.sumedh.moneytracker.domain.upi

import androidx.annotation.DrawableRes
import com.sumedh.moneytracker.R

object UpiAppIcons {
    @DrawableRes
    fun iconRes(app: UpiApp): Int = when (app) {
        UpiApp.GPAY -> R.drawable.ic_upi_gpay
        UpiApp.BHIM -> R.drawable.ic_upi_bhim
        UpiApp.PHONEPE -> R.drawable.ic_upi_phonepe
        UpiApp.POP -> R.drawable.ic_upi_pop
        UpiApp.PAYTM -> R.drawable.ic_upi_paytm
    }
}
