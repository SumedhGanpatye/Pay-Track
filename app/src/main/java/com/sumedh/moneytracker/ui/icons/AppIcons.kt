package com.sumedh.moneytracker.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.sumedh.moneytracker.R

/**
 * Icons from material-icons-core plus small custom vectors for flash / gallery / categories.
 */
object AppIcons {
    val Home: ImageVector = Icons.Outlined.Home
    val Analytics: ImageVector = Icons.AutoMirrored.Outlined.List
    val Settings: ImageVector = Icons.Outlined.Settings
    val Check: ImageVector = Icons.Outlined.Check
    val Edit: ImageVector = Icons.Outlined.Edit
    val ArrowBack: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    val ChevronRight: ImageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight
    val ExpandLess: ImageVector = Icons.Outlined.KeyboardArrowUp
    val ExpandMore: ImageVector = Icons.Outlined.KeyboardArrowDown
    val Person: ImageVector = Icons.Outlined.Person
    val Wallet: ImageVector = Icons.Outlined.AccountBox
    val DateRange: ImageVector = Icons.Outlined.DateRange
    val Refresh: ImageVector = Icons.Outlined.Refresh
    val More: ImageVector = Icons.Outlined.MoreVert

    @DrawableRes
    fun flashRes(torchOn: Boolean): Int =
        if (torchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off

    @Composable
    fun flash(torchOn: Boolean): ImageVector =
        ImageVector.vectorResource(flashRes(torchOn))

    @Composable
    fun gallery(): ImageVector =
        ImageVector.vectorResource(R.drawable.ic_gallery)

    @Composable
    fun category(category: String): ImageVector {
        val res = categoryDrawable(category)
        return if (res != null) {
            ImageVector.vectorResource(res)
        } else {
            categoryFallback(category)
        }
    }

    @DrawableRes
    fun categoryDrawable(category: String): Int? = when (category.trim().lowercase()) {
        "food" -> R.drawable.ic_category_food
        "coffee", "cafe", "café" -> R.drawable.ic_category_coffee
        "gym", "fitness", "workout" -> R.drawable.ic_category_gym
        "medicine", "medicines", "medical", "health", "pharmacy" ->
            R.drawable.ic_category_medicine
        else -> null
    }

    fun categoryFallback(category: String): ImageVector = when (category.trim().lowercase()) {
        "travel" -> Icons.Outlined.Place
        "shopping" -> Icons.Outlined.ShoppingCart
        "fuel", "petrol", "diesel" -> Icons.Outlined.Warning
        "entertainment", "movies" -> Icons.Outlined.Star
        "bills", "utilities", "rent" -> Icons.Outlined.Home
        "education", "books" -> Icons.Outlined.Info
        else -> Icons.Outlined.MoreVert
    }
}
