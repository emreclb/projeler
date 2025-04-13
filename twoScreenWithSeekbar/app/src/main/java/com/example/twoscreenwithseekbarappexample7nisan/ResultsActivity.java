package com.example.twoscreenwithseekbarappexample7nisan;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class ResultsActivity extends AppCompatActivity {

    ListView listViewResults;
    Button btnGeriDon;

    ArrayList<String> gelenSonuclar;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        listViewResults = findViewById(R.id.listViewResults);
        btnGeriDon = findViewById(R.id.btnGeriDon);

        // MainActivity'den gönderilen veriyi al
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("sonuclar")) {
            gelenSonuclar = intent.getStringArrayListExtra("sonuclar");
        } else {
            // Hata durumu veya veri yoksa boş liste oluştur
            gelenSonuclar = new ArrayList<>();
        }

        // ListView için Adapter kurulumu
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, // Standart liste görünümü
                gelenSonuclar);
        listViewResults.setAdapter(adapter);

        // Geri Dön Butonu Olayı
        btnGeriDon.setOnClickListener(v -> {
            // MainActivity'ye geri dön (MainActivity'deki onResume tetiklenecek)
            // Intent'e ekstra veri eklemeye gerek yok, çünkü MainActivity kendini resetliyor.
            finish(); // Bu aktiviteyi kapat, önceki (MainActivity) açılacak
        });

        // Oyun Bitti Kontrolü (3 doğru cevap olduysa)
        if (gelenSonuclar.size() >= 3) {
            showGameOverDialog();
        }
    }

    // Oyun Bitti Dialogunu Göster
    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Oyun Bitti!")
                .setMessage("Tebrikler, 3 ili doğru bildiniz.")
                .setPositiveButton("Tamam", (dialog, which) -> {
                    // Tamam'a basılınca uygulamayı kapat
                    finishAffinity(); // İlişkili tüm aktiviteleri kapatır
                })
                .setCancelable(false) // Dışarı tıklayarak veya geri tuşuyla kapatmayı engelle
                .show();
    }
}