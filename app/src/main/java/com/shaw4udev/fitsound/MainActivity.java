package com.shaw4udev.fitsound;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.media.MediaPlayer;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // VISTAS
    private EditText etMinutos;
    private Button btnCardio, btnFuerza, btnFlexibilidad;
    private Spinner spinnerGrafico;
    private BarChart barChart;
    private PieChart pieChart;
    private LineChart lineChart;

    // ESTADO HU5
    private float cardioMin = 0f;
    private float fuerzaMin = 0f;
    private float flexibilidadMin = 0f;
    private final List<Float> historialTotales = new ArrayList<>();

    // MediaPlayer
    private MediaPlayer mediaPlayer;

    //KEYS necesarias para guardar estados y demas en BUNDLE
    private static final String KEY_CARDIO = "cardioMin";
    private static final String KEY_FUERZA = "fuerzaMin";
    private static final String KEY_FLEX = "flexibilidadMin";
    private static final String KEY_HISTORIAL = "historial";
    private static final String KEY_SPINNER_POS = "spinnerPos";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //RESTAURAR ESTADO SI EXISTE (PARTE DE: HU5)
        if (savedInstanceState !=null){
            cardioMin = savedInstanceState.getFloat(KEY_CARDIO, 0f);
            fuerzaMin = savedInstanceState.getFloat(KEY_FUERZA, 0f);
            flexibilidadMin = savedInstanceState.getFloat(KEY_FLEX, 0f);

            //Convertir los datos de vuelta tomando lo que saco del bundle KEY_HISTORIAl y guardar asi un ArrayList
            float[] hist = savedInstanceState.getFloatArray(KEY_HISTORIAL);
            if (hist !=null){
                for (float v : hist) historialTotales.add(v);
            }
        }

        // REFERENCIAS A VISTAS DEL XML
        etMinutos = findViewById(R.id.etMinutos);
        btnCardio = findViewById(R.id.btnCardio);
        btnFuerza = findViewById(R.id.btnFuerza);
        btnFlexibilidad =findViewById(R.id.btnFlexibilidad);
        spinnerGrafico = findViewById(R.id.spinnerGrafico);
        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        lineChart = findViewById(R.id.lineChart);

        //CONFIGURACION DE SPINNER
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Barras", "Pastel", "Lineas"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrafico.setAdapter(adapter);

        //RESTAURAR POSICION DEL SPINNER
        if (savedInstanceState !=null);{
            spinnerGrafico.setSelection(savedInstanceState.getInt(KEY_SPINNER_POS, 0));
        }

        spinnerGrafico.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mostrarGrafico(position);
            }

            //Si, no va nada aqui
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //Iniciarlizar MediaPlayer HU3
        mediaPlayer = mediaPlayer.create(this, R.raw.exito);

        // LISTENER DE BOTONES
        btnCardio.setOnClickListener(v -> registrar("cardio"));
        btnFuerza.setOnClickListener(v -> registrar("fuerza"));
        btnFlexibilidad.setOnClickListener(v -> registrar("flexibilidad"));

        //Por aqui va el mapa



        //Dibujar grafico inicial con datos restaurados o vacio
        mostrarGrafico(spinnerGrafico.getSelectedItemPosition());
    }

    //LOGICA DE REGISTRO HU1
    private void registrar(String categoria){
        String texto = etMinutos.getText().toString().trim();

        //Validacion
        if (texto.isEmpty()){
            Toast.makeText(this, "Ingrese minutos validos", Toast.LENGTH_SHORT).show();
            return;
        }

        float minutos;
        try {
            minutos = Float.parseFloat(texto);
        }catch (NumberFormatException e){
            Toast.makeText(this, "Ingrese minutos validos", Toast.LENGTH_SHORT).show();
            return;
        }

        //Validacion debe ser >0
        if (minutos <= 0){
            Toast.makeText(this, "Ingrese minutos valids", Toast.LENGTH_SHORT).show();
            return;
        }

        //ACUMULAR SEGUN CATEGORIA
        switch (categoria){
            case "cardio": cardioMin += minutos; break;
            case "fuerza": fuerzaMin += minutos; break;
            case  "flexibilidad": flexibilidadMin += minutos; break;
        }

        //GUARDAR TOTAL EN HISTORIAL MAXIMO 5 VALORES
        float totalActual = cardioMin + fuerzaMin + flexibilidadMin;
        if (historialTotales.size() >=5) {
            historialTotales.remove(0);
        }
        historialTotales.add(totalActual);

        //Limpiar campo
        etMinutos.setText("");
        //Reproducir sonido
        reproducirSonido();
        //Actualizar grafico
        mostrarGrafico(spinnerGrafico.getSelectedItemPosition());
    }

    private void reproducirSonido(){
        if (mediaPlayer !=null){
            if (mediaPlayer.isPlaying()){
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();
        }
    }

    // ── HU2 – Control de visibilidad de gráficos ─────────────────────────────────
    private void mostrarGrafico(int position) {
        barChart.setVisibility(View.GONE);
        pieChart.setVisibility(View.GONE);
        lineChart.setVisibility(View.GONE);

        switch (position) {
            case 0:
                barChart.setVisibility(View.VISIBLE);
                actualizarBarras();
                break;
            case 1:
                pieChart.setVisibility(View.VISIBLE);
                actualizarPastel();
                break;
            case 2:
                lineChart.setVisibility(View.VISIBLE);
                actualizarLineas();
                break;
        }
    }

    // GRAFICA DE BARRAS
    private void actualizarBarras() {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, cardioMin));
        entries.add(new BarEntry(1f, fuerzaMin));
        entries.add(new BarEntry(2f, flexibilidadMin));

        BarDataSet dataSet = new BarDataSet(entries, "Minutos por categoría");
        dataSet.setColors(
                0xFF4CAF50, // verde – Cardio
                0xFF2196F3, // azul  – Fuerza
                0xFFFF9800  // naranja – Flexibilidad
        );
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(new String[]{"Cardio", "Fuerza", "Flexibilidad"})
        );
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setLabelCount(3);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(true);
        barChart.animateY(400);
        barChart.invalidate();
    }

    //GRAFICA DE PASTEL


    //GRAFICA DE LINEAS


    // CALLBACK DEL MAPA


    //GUARDAR ESTADO ANTES DE ROTAR


    //LIBERAR MEDIAPLAYER AL DESTRUIR LA ACTIVIDAD
}