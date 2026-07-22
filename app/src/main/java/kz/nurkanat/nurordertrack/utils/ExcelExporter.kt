package kz.nurkanat.nurordertrack.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.Order
import kz.nurkanat.nurordertrack.data.model.OrderItem
import kz.nurkanat.nurordertrack.data.model.OrderStatus
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

    suspend fun exportOrders(
        context: Context,
        orders: List<Order>,
        getItems: suspend (String) -> List<OrderItem>
    ): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(context.getString(R.string.excel_sheet_name))
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.ROYAL_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
                fontHeightInPoints = 11
            }
            setFont(font)
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val dataStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            wrapText = true
        }

        val totalStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
        }

        val subItemStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LEMON_CHIFFON.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            indention = 2
        }

        val headers = listOf(
            context.getString(R.string.excel_col_order_num),
            context.getString(R.string.excel_col_client),
            context.getString(R.string.excel_col_phone),
            context.getString(R.string.excel_col_executor),
            context.getString(R.string.excel_col_status),
            context.getString(R.string.excel_col_product),
            context.getString(R.string.excel_col_qty),
            context.getString(R.string.excel_col_price),
            context.getString(R.string.excel_col_item_sum),
            context.getString(R.string.excel_col_total),
            context.getString(R.string.excel_col_comment),
            context.getString(R.string.excel_col_date)
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, title ->
            headerRow.createCell(i).apply {
                setCellValue(title)
                cellStyle = headerStyle
            }
        }

        var rowIndex = 1

        orders.forEach { order ->
            val items = getItems(order.id)
            if (items.isEmpty()) {
                val row = sheet.createRow(rowIndex++)
                fillOrderRow(
                    context = context,
                    row = row,
                    order = order,
                    item = null,
                    dateFormat = dateFormat,
                    style = dataStyle,
                    totalStyle = totalStyle
                )
            } else {
                items.forEachIndexed { index, item ->
                    val row = sheet.createRow(rowIndex++)
                    val isFirst = index == 0
                    fillOrderRow(
                        context = context,
                        row = row,
                        order = if (isFirst) order else null,
                        item = item,
                        dateFormat = dateFormat,
                        style = if (isFirst) dataStyle else subItemStyle,
                        totalStyle = totalStyle,
                        isFirst = isFirst
                    )
                }
            }
        }

        val columnWidths = listOf(4000, 6000, 4000, 5000, 4000, 8000, 3000, 4000, 4000, 4500, 6000, 5000)
        columnWidths.forEachIndexed { i, width -> sheet.setColumnWidth(i, width) }

        val totalRow = sheet.createRow(rowIndex)
        totalRow.createCell(0).apply {
            setCellValue(context.getString(R.string.excel_total_row))
            cellStyle = totalStyle
        }
        totalRow.createCell(9).apply {
            setCellValue(orders.sumOf { it.totalAmount })
            cellStyle = totalStyle
        }
        (1..8).forEach { i -> totalRow.createCell(i).cellStyle = totalStyle }
        (10..11).forEach { i -> totalRow.createCell(i).cellStyle = totalStyle }

        val fileName = "NurOrderTrack_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()

        return file
    }

    private fun fillOrderRow(
        context: Context,
        row: org.apache.poi.ss.usermodel.Row,
        order: Order?,
        item: OrderItem?,
        dateFormat: SimpleDateFormat,
        style: CellStyle,
        totalStyle: CellStyle,
        isFirst: Boolean = true
    ) {
        val statusLabel = when (order?.status) {
            OrderStatus.NEW -> context.getString(R.string.status_new)
            OrderStatus.IN_PROGRESS -> context.getString(R.string.status_in_progress)
            OrderStatus.DONE -> context.getString(R.string.status_done)
            OrderStatus.CLOSED -> context.getString(R.string.status_closed)
            OrderStatus.CANCELLED -> context.getString(R.string.status_cancelled)
            null -> ""
        }

        fun cell(index: Int, value: String = "", numValue: Double? = null) {
            row.createCell(index).apply {
                if (numValue != null) setCellValue(numValue)
                else setCellValue(value)
                cellStyle = style
            }
        }

        if (order != null && isFirst) {
            cell(0, if (order.orderNumber > 0) "#${order.orderNumber}" else order.id.takeLast(8).uppercase())
            cell(1, order.clientName)
            cell(2, order.clientPhone)
            cell(3, order.assignedToName)
            cell(4, statusLabel)
        } else {
            (0..4).forEach { cell(it) }
        }

        if (item != null) {
            cell(5, item.productName)
            cell(6, numValue = item.quantity)
            cell(7, numValue = item.price)
            cell(8, numValue = item.total)
        } else {
            (5..8).forEach { cell(it) }
        }

        if (order != null && isFirst) {
            row.createCell(9).apply {
                setCellValue(order.totalAmount)
                cellStyle = totalStyle
            }
            cell(10, order.comment)
            cell(11, dateFormat.format(order.createdAt.toDate()))
        } else {
            (9..11).forEach { cell(it) }
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.excel_share_title))
        )
    }
}