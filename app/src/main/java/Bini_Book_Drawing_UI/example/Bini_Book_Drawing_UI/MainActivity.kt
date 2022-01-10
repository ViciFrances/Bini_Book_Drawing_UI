package Bini_Book_Drawing_UI.example.Bini_Book_Drawing_UI

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Bini_Book_Drawing_UI.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? =
            null // Variable, in dem der aktuell ausgewählte ImageButton der Farben gespeichert wird

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Methode zum verstecken der Statusleiste
        hideStatusBar()

        drawing_view.setSizeForBrush(20.toFloat()) // Standardwert für Strichstärke zu Anfang setzen.

        /**
         * Hier wählen wir die Anfangsfarbe aus. Dabei wollen wir schwarz als Anfangsfarbe setzen. Die Farbe ist
         * die zweite in der Farbpallete, also wählen wir die Position 1 im Array, weil dieses bei 0 anfängt zu zählen.
         * Außerdem setzen wir für den ImageButton den aktiven Hintergrund.
         */
        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                        this,
                    R.drawable.pallet_pressed
                )
        )

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener {
            //Prüfen, ob App Berechtigungen hat
            if (isReadStorageAllowed()) {
                // Aufruf der Gallerie, um ein Bild auszuwählen.
                val pickPhoto = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhoto, GALLERY)
            } else {

                //Falls keine Berechtigung vorliegt, wird diese abgefragt
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener {
            // Rückgängig machen aufrufen.
            drawing_view.onClickUndo()
        }

        ib_save.setOnClickListener {

            // Besteht Berechtigung?
            if (isReadStorageAllowed()) {

                BitmapCoroutine(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {

                // Wenn keine Berechtigungen vorliegen, müssen diese abgefragt werden.
                requestStoragePermission()
            }
        }
    }

    /**
     * Überschreibungsfunktion, die aufgerufen wird, nachdem Berechtigung abgefragtr wurde
     *
     * Die Funktion wird aufgerufen und enthält die Daten über die Berechtigungsabfrage. Hier können wir
     * die Daten auswerten.
     *
     * @param activity Aktuelle Activity.
     * @param permissions Berechtigung, die abgefragt werden soll. Ist nicht null oder leer.
     * @param requestCode Anfragen-Code, den wir bei Berechtigungsanfrage mitgegeben haben
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        // Anfragen-Code der Berechtigung. So kann bei mehreren Abfragen von Berechtigungen eindeutig auf die jeweilige
        // Anfrage reagiert werden
        if (requestCode == STORAGE_PERMISSION_CODE) {

            //Wenn Berechtigung erteilt wird
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        this@MainActivity,
                        "Berechtigung erteilt.",
                        Toast.LENGTH_LONG
                ).show()
            } else {
                // Toast anzeigen, falls Berechtigung abgelehnt wurde
                Toast.makeText(
                        this@MainActivity,
                        "Du hast die Berechtigung abgelehnt.",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Überschreibungsfunktion, die aufgerufen wird, wenn eine ActivityForResult aufgerufen wurde und diese ein Ergebnis liefert.
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {

                        // Wenn Nutzer ein Bild auswählt, soll ImageView sichtbar werden.
                        iv_background.visibility = View.VISIBLE

                        // Ausgewähltes Bild als Hintergrund setzen.
                        iv_background.setImageURI(data.data)
                    } else {
                        // Wenn das ausgewählte Bild ungültig ist oder keines ausgewählt wurde
                        Toast.makeText(
                                this@MainActivity,
                                "Fehler bei der Auswahl eines Bildes.",
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Methode wird aufgerufen, um einen Dialog mit verschiedenen Strichstärken anzuzeigen.
     */
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Strichstärke :")
        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }

    /**
     * Methode wird aufgerufen, wenn ein Button aus der Farbpalette geklickt wird.
     *
     * @param view ImageButton der angeklickt wurde.
     */
    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            // Aktualisierung der Farbe
            val imageButton = view as ImageButton
            // Tag wird benutzt, um die aktuelle Farbe und die zuvor ausgewählte Farbe zu tauscchen.
            // Tag speichert aktuelles View
            val colorTag = imageButton.tag.toString()
            // Unserer colorTag wird als Farbe gesetzt.
            drawing_view.setColor(colorTag)
            // Hintergrund des letzten aktiven und des aktiven Buttons ändern.
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(
                            this,
                        R.drawable.pallet_normal
                    )
            )

            // Die aktuelle View wird durch die übergebene View (ImageButton) aktualisiert.
            mImageButtonCurrentPaint = view
        }
    }

    /**
     * Berechtigungsanfrage
     */
    private fun requestStoragePermission() {

        /**
         * Prüft, ob eine weitere Erklärung zu der Berechtigung angegeben werden soll. Falls klar ist,
         * warum beispielsweise die Kamera bei einer Kamera-App benötigt wird, sollte diese Erklärung
         * nicht angezeigt werden. Falls zusätzlich der Standort abgefragt wird, macht es vielleicht Sinn
         * zu erklären, dass dadurch ein Bild kategorisiert werden soll etc.
         *
         * @param activity Aktuelle Activity.
         * @param permission Die Berechtigung, die abgefragt werden soll.
         * @return Ob Berechtigungsdialog/ Erklärung angezeigt werden soll.
         *
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ).toString()
                )
        ) {
            // Wenn der Nutzer die Berechtigung abgelehnt hat, springt der Code in diesen Block und
            // du kannst dem Nutzer erklären, weshalb du die Berechtigung benötigst etc.
        }

        /**
         * Abfrage der Berechtigung. Diese muss im Manifest aufgeführt werden.
         */

        // Berechtigung abfragen
        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
                STORAGE_PERMISSION_CODE
        )
    }

    /**
     * Methode aufrufen, um Status der Berechtigung abzufragen
     */
    private fun isReadStorageAllowed(): Boolean {
        // Status der Berechtigung abfragen

        /**
         * @param permission Name der Berechtigung, die geprüft werden soll.
         *
         */
        val result = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        // Wenn Berechtigungen erteilt wurden, wird true zurückgegeben, sonst false
        return result == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Bitmap aus der View erstellen
     */
    private fun getBitmapFromView(view: View): Bitmap {

        // Bitmap mit der gleichen Größe, wie die der View, erstellen
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        // Hintergrund der View bekommen
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //Wenn Hintergrunddatei vorhanden ist, soll diese gezeichnet werden
            bgDrawable.draw(canvas)
        } else {
            // Wenn keine Hintergrunddatei, dann weiße Farbe
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        // Rückgabe der Bitmap
        return returnedBitmap
    }

    /*
     * Für Hintergrund-Aktivität nutzen wir die Coroutine
     *
     * Coroutine : Aktivität im Hintergrund, ohne dass der Bildschirm einfriert.
     */
    private inner class BitmapCoroutine(val mBitmap: Bitmap?) :
            ViewModel() {

        /**
         * Variable, um mProgressDialog anzuzeigen. Diese initialisieren wir später.
         */
        private lateinit var mProgressDialog: Dialog

        fun onPreExecute() {
            showProgressDialog()
        }

        fun execute() = viewModelScope.launch {
            onPreExecute()
            val result = doInBackground()
            onPostExecute(result)
        }


        private suspend fun doInBackground(vararg params: Any): String=withContext(Dispatchers.Default) {

            var result = ""

            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream() // Erstellt einen Array-Output Stream

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    // Komprimierte Form der Bitmap soll in den OutputStream geschrieben werden.

                    val f = File(
                            externalCacheDir!!.absoluteFile.toString()
                                    + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Environment : Zugriff auf Envoronment Variablen.
                    // getExternalStorageDirectory : Gibt den primären Dateipfad für den externen Speicher.
                    // absoluteFile : Absoluter Pfadname.
                    // File.separator : Standard Seperator, basierend auf dem System.

                    val fo =
                            FileOutputStream(f) // Datei-Ausgabestream erstellen und das Objekt f mitgeben.
                    fo.write(bytes.toByteArray()) // Schreibt die bytes aus dem ArrayOutputStream in den FileOutputStream.
                    fo.close() // Schließt den Ausgabestream.
                    result = f.absolutePath // Absoluter Pfad der Datei wird im Result gespeichert.
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return@withContext result
        }

        fun onPostExecute(result: String) {
            cancelProgressDialog()
            if (!result.isEmpty()) {
                Toast.makeText(
                        this@MainActivity,
                        "Erfolgreich gespeichert :$result",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                        this@MainActivity,
                        "Fehler beim Speichern aufgetreten.",
                        Toast.LENGTH_SHORT
                ).show()
            }

            // TODO (Schritt 1 - Teilen des gezeichneten Bildes)
            // START

            /*
            Die MediaScannerConnection ermöglicht es uns, die neu erstellte Datei zu teilen. Dazu
            übergeben wir das result, also den Dateipfad und die MediaScannerConnection scanned die
            Datei ein und bekommt zusätzlich Metadaten.*/

            MediaScannerConnection.scanFile(
                    this@MainActivity, arrayOf(result), null
            ) { path, uri ->
                // Nachdem das Bild gespeichert wurde, kann dieses geteilt werden.
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                ) // URI (unified ressource identifier) enthält Daten über die zu versendende Datei
                shareIntent.type =
                        "image/jpeg" // Der Datei-Typ der versendet werden soll
                startActivity(
                        Intent.createChooser(
                                shareIntent,
                                "Teilen"
                        )
                )
            }
            // ENDE
        }

        /**
         * Funktion zeigt den Ladedialog an, sodass der Nutzer sieht, dass etwas im Hintergrund passiert.
         */
        private fun showProgressDialog() {
            mProgressDialog = Dialog(this@MainActivity)

            /*Content des Screens als unsere Layout-Datei setzen*/
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)

            // Ladedialog starten und anzeigen
            mProgressDialog.show()
        }

        /**
         * Funktion beendet den Ladedialog.
         */
        private fun cancelProgressDialog() {
            mProgressDialog.dismiss()
        }
    }

    companion object{


        /**
         * Berechtigungscode, der in der Methode onRequestPermissionsResult abgefragt wird
         *
         * Für weitere Informationen: https://developer.android.com/training/permissions/requesting#kotlin
         */
        private const val STORAGE_PERMISSION_CODE = 1
        // Code, um zu identifizieren, dass Galerie geöffnet wurde.
        private const val GALLERY = 2
    }

    private fun hideStatusBar(){
    if(Build.VERSION.SDK_INT < 30) {

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }else {
        window.setDecorFitsSystemWindows(false)
        val controller = window.insetsController
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    }
}