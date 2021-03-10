package org.bilalkilic.kediatrhelper.utils

import java.awt.event.MouseEvent
import javax.swing.Icon

data class KediatrPopupModel(
    val title: String,
    val items: Collection<PopupItem>,
    val icon: Icon? = null,
    val onChosenFunction: (PopupItem) -> Unit,
    val mouseEvent: MouseEvent,
)
