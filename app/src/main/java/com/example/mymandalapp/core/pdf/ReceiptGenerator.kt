package com.example.mymandalapp.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.example.mymandalapp.R
import com.example.mymandalapp.core.finance.Money
import com.example.mymandalapp.core.finance.Transaction
import com.example.mymandalapp.core.mandal.MandalProfile
import com.example.mymandalapp.data.repository.LocalBrandingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object ReceiptGenerator {

    // A4 Standard Dimensions (in points: 1/72 inch)
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f

    /**
     * Generates a professional full-page A4 donation receipt PDF.
     * Fixed scaling issues by using point-based dimensions throughout.
     */
    suspend fun generateDonationReceipt(
        context: Context,
        transaction: Transaction,
        profile: MandalProfile
    ): Uri? = withContext(Dispatchers.Main) { // UI operations must be on Main
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // 1. Inflate and Bind Data
        val view = LayoutInflater.from(context).inflate(R.layout.receipt_template, null)

        view.findViewById<TextView>(R.id.tvMandalName).text = profile.mandalName.uppercase()
        view.findViewById<TextView>(R.id.tvLocation).text = profile.location.uppercase()
        view.findViewById<TextView>(R.id.tvReceiptNumber).text = "Receipt No: ${transaction.receiptNumber ?: "N/A"}"
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        view.findViewById<TextView>(R.id.tvReceiptDate).text = "Date: ${dateFormat.format(transaction.date.toDate())}"
        
        view.findViewById<TextView>(R.id.tvDonorName).text = (transaction.donorName ?: "Anonymous").uppercase()
        view.findViewById<TextView>(R.id.tvAmount).text = Money.format(transaction.amountPaise)
        view.findViewById<TextView>(R.id.tvAmountInWords).text = "(${Money.toWords(transaction.amountPaise)})"
        view.findViewById<TextView>(R.id.tvPaymentMode).text = "Payment Mode: ${transaction.paymentMode}"
        
        val tvMobile = view.findViewById<TextView>(R.id.tvMobile)
        if (!transaction.donorMobile.isNullOrBlank()) {
            tvMobile.text = "Mobile: ${transaction.donorMobile}"
            tvMobile.visibility = View.VISIBLE
        } else {
            tvMobile.visibility = View.GONE
        }
        
        view.findViewById<TextView>(R.id.tvTreasurerName).text = profile.treasurerName.uppercase().ifBlank { "TREASURER" }

        // 2. Branding
        val brandingRepo = LocalBrandingRepository(context)
        val logoBitmap = withContext(Dispatchers.IO) { brandingRepo.getBrandingBitmap("logo") }
        logoBitmap?.let {
            view.findViewById<ImageView>(R.id.ivGanapati).setImageBitmap(it)
        }
        
        val ivStamp = view.findViewById<ImageView>(R.id.ivStamp)
        val stampBitmap = withContext(Dispatchers.IO) { brandingRepo.getBrandingBitmap("stamp") }
        
        stampBitmap?.let {
            ivStamp.setImageBitmap(it)
            ivStamp.visibility = View.VISIBLE
        } ?: run {
            ivStamp.visibility = View.GONE
        }

        // 3. MEASURE & LAYOUT (CRITICAL FOR SCALING)
        // Convert A4 points to pixels based on the system's current 'pt' interpretation.
        // This ensures the XML layout is measured at exactly the same scale it will be drawn.
        val ptScale = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 1f, context.resources.displayMetrics)
        
        val widthPx = (PAGE_WIDTH * ptScale).toInt()
        val heightPx = (PAGE_HEIGHT * ptScale).toInt()
        
        // Use exact A4 dimensions for measurement
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        
        view.measure(widthMeasureSpec, heightMeasureSpec)
        view.layout(0, 0, widthPx, heightPx)

        // 4. DRAW TO PDF
        // Since the view was measured in pixels but PDF canvas works in points,
        // we must scale the canvas down to match the 1:1 point coordinate system.
        canvas.save()
        val scale = 1f / ptScale
        canvas.scale(scale, scale)
        view.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)

        // 5. SAVE
        val fileName = "receipt_${transaction.receiptNumber?.replace("-", "_") ?: transaction.id}.pdf"
        val file = File(context.cacheDir, fileName)
        
        return@withContext withContext(Dispatchers.IO) {
            try {
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                e.printStackTrace()
                pdfDocument.close()
                null
            }
        }
    }
}
