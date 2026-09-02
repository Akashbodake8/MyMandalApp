package com.example.mymandalapp.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.finance.TransactionType
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.data.repository.LocalBrandingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    private const val MARGIN = 45f
    private const val FOOTER_HEIGHT = 40f
    private const val PAGE_WIDTH = 595 // A4 Width
    private const val PAGE_HEIGHT = 842 // A4 Height

    suspend fun generateFullReport(
        context: Context,
        profile: MandalProfile,
        year: String,
        openingBalance: Long,
        transactions: List<Transaction>
    ): Uri? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        
        val sortedTransactions = transactions.sortedWith(compareBy({ it.date }, { it.createdAt }))
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val generatedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        var pageNumber = 1
        var page = startNewPage(pdfDocument, pageNumber)
        var canvas = page.canvas
        var y = MARGIN

        fun checkPageBreak(requiredHeight: Float) {
            if (y + requiredHeight > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) {
                drawFooter(canvas, pageNumber, profile.mandalName, year)
                pdfDocument.finishPage(page)
                pageNumber++
                page = startNewPage(pdfDocument, pageNumber)
                canvas = page.canvas
                y = MARGIN
                drawHeaderOnEveryPage(canvas, profile.mandalName, year, generatedDate, textPaint, linePaint)
                y = MARGIN + 40f
            }
        }

        // 1. First Page Header
        drawFullHeader(context, canvas, profile, year, generatedDate, textPaint, linePaint)
        y = 190f

        // 2. Financial Summary
        checkPageBreak(150f)
        y = drawFinancialSummary(canvas, y, openingBalance, sortedTransactions, textPaint, linePaint)
        y += 30f

        // 3. Monetary Transaction Table
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText("MONETARY TRANSACTIONS", MARGIN, y, textPaint)
        y += 20f

        val cols = listOf(MARGIN, MARGIN + 55, MARGIN + 125, MARGIN + 265, MARGIN + 310, MARGIN + 375, MARGIN + 440)
        val colWidths = listOf(50f, 65f, 135f, 40f, 60f, 60f, 65f)
        val headers = listOf("Date", "Record No", "Description/Donor", "Mode", "Income", "Expense", "Balance")
        
        y = drawTableHeader(canvas, y, cols, headers, textPaint, linePaint)
        
        var runningBalance = openingBalance
        sortedTransactions.forEach { tx ->
            if (tx.type == TransactionType.OBJECT_DONATION) return@forEach

            if (tx.type == TransactionType.INCOME) runningBalance += tx.amountPaise
            if (tx.type == TransactionType.EXPENSE) runningBalance -= tx.amountPaise

            val rowData = listOf(
                dateFormat.format(tx.date.toDate()),
                tx.receiptNumber ?: (if (tx.type == TransactionType.EXPENSE) "EXP" else "TXN"),
                valText(tx),
                tx.paymentMode.name,
                if (tx.type == TransactionType.INCOME) Money.format(tx.amountPaise) else "—",
                if (tx.type == TransactionType.EXPENSE) Money.format(tx.amountPaise) else "—",
                Money.format(runningBalance)
            )

            val descLayout = createStaticLayout(rowData[2], textPaint, colWidths[2].toInt(), 9f)
            val rowHeight = Math.max(25f, descLayout.height.toFloat() + 10f)

            checkPageBreak(rowHeight)
            if (y == MARGIN) {
                y = MARGIN + 40f
                y = drawTableHeader(canvas, y, cols, headers, textPaint, linePaint)
            }

            y = drawTableRow(canvas, y, cols, rowData, descLayout, textPaint, tx.edited)
        }
        y += 30f

        // 4. Object Donations Section
        val objectDonations = sortedTransactions.filter { it.type == TransactionType.OBJECT_DONATION }
        if (objectDonations.isNotEmpty()) {
            checkPageBreak(100f)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 12f
            canvas.drawText("OBJECT DONATIONS (IN-KIND)", MARGIN, y, textPaint)
            y += 20f

            val oCols = listOf(MARGIN, MARGIN + 60, MARGIN + 130, MARGIN + 260, MARGIN + 350, MARGIN + 430)
            val oWidths = listOf(55f, 65f, 125f, 85f, 75f, 75f)
            val oHeaders = listOf("Date", "ID", "Item Name", "Quantity", "Donor", "Est. Value")
            
            y = drawTableHeader(canvas, y, oCols, oHeaders, textPaint, linePaint)

            objectDonations.forEach { obj ->
                val oRowData = listOf(
                    dateFormat.format(obj.date.toDate()),
                    "OBJ",
                    obj.itemName ?: "",
                    "${obj.quantity} ${obj.unit}",
                    obj.donorName ?: "",
                    obj.estimatedValuePaise?.let { Money.format(it) } ?: "—"
                )

                val itemLayout = createStaticLayout(oRowData[2], textPaint, oWidths[2].toInt(), 9f)
                val oRowHeight = Math.max(25f, itemLayout.height.toFloat() + 10f)

                checkPageBreak(oRowHeight)
                if (y == MARGIN) {
                    y = MARGIN + 40f
                    y = drawTableHeader(canvas, y, oCols, oHeaders, textPaint, linePaint)
                }

                y = drawTableRow(canvas, y, oCols, oRowData, itemLayout, textPaint, obj.edited)
            }
        }

        // Draw Stamp on last page
        checkPageBreak(120f)
        val brandingRepo = LocalBrandingRepository(context)
        val stampBitmap = brandingRepo.getBrandingBitmap("stamp")
        if (stampBitmap != null) {
            canvas.drawBitmap(stampBitmap, null, RectF(PAGE_WIDTH - MARGIN - 100f, y, PAGE_WIDTH - MARGIN, y + 80f), textPaint)
            y += 90f
        }
        
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText("Treasurer: ${profile.treasurerName}", PAGE_WIDTH - MARGIN, y, textPaint)

        drawFooter(canvas, pageNumber, profile.mandalName, year)
        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Mandal_Report_${year}_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun startNewPage(pdfDocument: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        return pdfDocument.startPage(pageInfo)
    }

    private fun drawFullHeader(context: Context, canvas: Canvas, profile: MandalProfile, year: String, date: String, paint: TextPaint, linePaint: Paint) {
        val brandingRepo = LocalBrandingRepository(context)
        val logoBitmap = brandingRepo.getBrandingBitmap("logo")
        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(PAGE_WIDTH / 2f - 40f, 20f, PAGE_WIDTH / 2f + 40f, 100f), paint)
        } else {
            drawDefaultLogo(context, canvas)
        }

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText(profile.mandalName.uppercase(), PAGE_WIDTH / 2f, 125f, paint)
        
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(profile.location, PAGE_WIDTH / 2f, 145f, paint)
        
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FINANCE REPORT — FESTIVAL $year", PAGE_WIDTH / 2f, 168f, paint)
        
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Generated on: $date", PAGE_WIDTH / 2f, 182f, paint)
        
        canvas.drawLine(MARGIN, 188f, PAGE_WIDTH - MARGIN, 188f, linePaint)
    }

    private fun drawDefaultLogo(context: Context, canvas: Canvas) {
        val logo = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_ganpati_emblem, null)
        logo?.let {
            it.setBounds(PAGE_WIDTH / 2 - 40, 20, PAGE_WIDTH / 2 + 40, 100)
            it.draw(canvas)
        }
    }

    private fun drawHeaderOnEveryPage(canvas: Canvas, mandal: String, year: String, date: String, paint: TextPaint, linePaint: Paint) {
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText(mandal, MARGIN, 30f, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Festival $year | $date", PAGE_WIDTH - MARGIN, 30f, paint)
        canvas.drawLine(MARGIN, 35f, PAGE_WIDTH - MARGIN, 35f, linePaint)
    }

    private fun drawFinancialSummary(canvas: Canvas, startY: Float, opening: Long, transactions: List<Transaction>, paint: TextPaint, linePaint: Paint): Float {
        var y = startY
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("FINANCIAL SUMMARY", MARGIN, y, paint)
        y += 25f

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
        val currentBalance = opening + totalIncome - totalExpense

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        
        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false) {
            if (isBold) paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(label, MARGIN + 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, PAGE_WIDTH - MARGIN - 20f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            y += 20f
        }

        drawSummaryRow("Opening Balance", Money.format(opening))
        drawSummaryRow("Total Monetary Donations", "+ " + Money.format(totalIncome))
        drawSummaryRow("Total Expenses", "− " + Money.format(totalExpense))
        
        y += 5f
        canvas.drawLine(PAGE_WIDTH / 2f, y, PAGE_WIDTH - MARGIN - 20f, y, linePaint)
        y += 20f
        
        drawSummaryRow("CURRENT BALANCE", Money.format(currentBalance), true)
        
        return y
    }

    private fun drawTableHeader(canvas: Canvas, startY: Float, cols: List<Float>, headers: List<String>, paint: TextPaint, linePaint: Paint): Float {
        var y = startY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.textAlign = Paint.Align.LEFT
        
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, cols[i], y, paint)
        }
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 15f
        return y
    }

    private fun drawTableRow(canvas: Canvas, startY: Float, cols: List<Float>, data: List<String>, descLayout: StaticLayout, paint: TextPaint, edited: Boolean): Float {
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.textAlign = Paint.Align.LEFT
        
        canvas.drawText(data[0], cols[0], startY, paint)
        canvas.drawText(data[1], cols[1], startY, paint)
        
        canvas.save()
        canvas.translate(cols[2], startY - 9f)
        descLayout.draw(canvas)
        canvas.restore()
        
        canvas.drawText(data[3], cols[3], startY, paint)
        if (data.size > 4) canvas.drawText(data[4], cols[4], startY, paint)
        if (data.size > 5) canvas.drawText(data[5], cols[5], startY, paint)
        if (data.size > 6) canvas.drawText(data[6], cols[6], startY, paint)
        
        if (edited) {
            paint.textSize = 6f
            paint.color = Color.DKGRAY
            canvas.drawText("(Edited)", cols[1], startY + 8f, paint)
            paint.color = Color.BLACK
            paint.textSize = 9f
        }

        return startY + Math.max(25f, descLayout.height.toFloat() + 10f)
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, mandal: String, year: String) {
        val paint = Paint().apply {
            textSize = 8f
            color = Color.GRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val footerY = PAGE_HEIGHT - 25f
        canvas.drawText("Generated by Ganpati Mandal Finance Management App | Mandal: $mandal | Year: $year", MARGIN, footerY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNum", PAGE_WIDTH - MARGIN, footerY, paint)
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int, textSize: Float): StaticLayout {
        paint.textSize = textSize
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun valText(tx: Transaction): String {
        return when (tx.type) {
            TransactionType.INCOME -> tx.donorName ?: "Donation"
            TransactionType.EXPENSE -> tx.description
            TransactionType.OPENING_BALANCE -> "Opening Balance"
            TransactionType.OBJECT_DONATION -> "Object Donation"
        }
    }
}
