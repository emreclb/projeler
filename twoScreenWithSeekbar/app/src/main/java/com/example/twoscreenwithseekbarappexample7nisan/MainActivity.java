package com.example.twoscreenwithseekbarappexample7nisan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper; // Handler için Looper ekleyin
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale; // Küçük harf dönüşümü için

public class MainActivity extends AppCompatActivity {

    // Arayüz Elemanları
    Button btnStartTimer, btnOnayla;
    TextView tvTimer, tvSecilenPlaka;
    SeekBar seekBarPlaka;
    EditText etIlAdi;

    // Sayaç Değişkenleri
    Handler timerHandler = new Handler(Looper.getMainLooper()); // Ana thread üzerinde çalışacak Handler
    long startTime = 0L;
    boolean isTimerRunning = false;
    int elapsedTimeInSeconds = 0; // Geçen süreyi saniye olarak tut

    // İl Plaka Verileri
    HashMap<Integer, String> ilPlakaMap;
    int currentPlaka = 1; // Başlangıçta seçili plaka

    // Sonuçlar (Bu listeyi Intent ile diğer aktiviteye taşıyacağız)
    // static ArrayList<String> sonuclarListesi = new ArrayList<>(); // Static yerine Intent kullanmak daha iyi
    ArrayList<String> sonuclarListesi = new ArrayList<>(); // Sonuçları bu aktivitede tutalım

    // Sayaç Güncelleme Görevi (Runnable)
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = SystemClock.uptimeMillis() - startTime;
            elapsedTimeInSeconds = (int) (millis / 1000);
            tvTimer.setText(String.valueOf(elapsedTimeInSeconds));

            // Her saniye kendini tekrar çağır
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Arayüz Elemanlarını Bağlama
        btnStartTimer = findViewById(R.id.btnStartTimer);
        btnOnayla = findViewById(R.id.btnOnayla);
        tvTimer = findViewById(R.id.tvTimer);
        tvSecilenPlaka = findViewById(R.id.tvSecilenPlaka);
        seekBarPlaka = findViewById(R.id.seekBarPlaka);
        etIlAdi = findViewById(R.id.etIlAdi);

        // İl Plaka Eşleşmelerini Hazırlama
        prepareIlPlakaMap();

        // Başlangıç Durumu
        tvSecilenPlaka.setText(String.valueOf(currentPlaka));
        disableGameControls(); // Başlangıçta oyun kontrolleri pasif

        // --- Dinleyiciler (Listeners) ---

        // Sayaç Başlatma Butonu
        btnStartTimer.setOnClickListener(v -> {
            if (!isTimerRunning) {
                startGame();
            }
        });

        // SeekBar Değişim Dinleyicisi
        seekBarPlaka.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPlaka = progress;
                tvSecilenPlaka.setText(String.valueOf(currentPlaka));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { } // Gerekmiyorsa boş bırakılabilir

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { } // Gerekmiyorsa boş bırakılabilir
        });

        // Onayla Butonu
        btnOnayla.setOnClickListener(v -> {
            checkAnswer();
        });
    }

    // Oyun kontrollerini aktifleştirme
    private void enableGameControls() {
        seekBarPlaka.setEnabled(true);
        etIlAdi.setEnabled(true);
        btnOnayla.setEnabled(true);
        btnStartTimer.setEnabled(false); // Başlat butonu pasifleşir
    }

    // Oyun kontrollerini pasifleştirme
    private void disableGameControls() {
        seekBarPlaka.setEnabled(false);
        etIlAdi.setEnabled(false);
        btnOnayla.setEnabled(false);
        btnStartTimer.setEnabled(true); // Başlat butonu aktifleşir
    }

    // Oyunu başlatma
    private void startGame() {
        startTime = SystemClock.uptimeMillis();
        isTimerRunning = true;
        timerHandler.postDelayed(timerRunnable, 0); // Sayacı başlat
        enableGameControls();
        etIlAdi.setText(""); // Önceki girişi temizle
        etIlAdi.requestFocus(); // Odaklanmayı EditText'e ver
    }

    // Sayacı durdurma
    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        isTimerRunning = false;
    }

    // Oyunu sıfırlama (bir sonraki tur veya geri dönüş için)
    private void resetGame() {
        stopTimer();
        elapsedTimeInSeconds = 0;
        tvTimer.setText("0");
        currentPlaka = 1; // SeekBar'ı başa al
        seekBarPlaka.setProgress(currentPlaka);
        tvSecilenPlaka.setText(String.valueOf(currentPlaka));
        etIlAdi.setText("");
        disableGameControls();
    }


    // Cevabı Kontrol Etme
    private void checkAnswer() {
        String userAnswer = etIlAdi.getText().toString().trim();
        // Türkçe karakterleri de içeren doğru küçük harf dönüşümü
        String userAnswerLower = userAnswer.toLowerCase(new Locale("tr", "TR"));

        String correctAnswer = ilPlakaMap.get(currentPlaka);

        // HashMap'ten null dönme ihtimaline karşı kontrol
        if (correctAnswer == null) {
            Toast.makeText(this, "Hata: Bu plaka için il bilgisi bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }


        if (userAnswerLower.equals(correctAnswer)) {
            // --- DOĞRU CEVAP ---
            stopTimer();

            // Sonucu formatla (İlk harf büyük)
            String formattedIlAdi = correctAnswer.substring(0, 1).toUpperCase(new Locale("tr", "TR")) + correctAnswer.substring(1);
            String result = formattedIlAdi + " - " + elapsedTimeInSeconds + " saniye";
            sonuclarListesi.add(result); // Sonucu listeye ekle

            // Sonuç Ekranına Geçiş
            Intent intent = new Intent(MainActivity.this, ResultsActivity.class);
            intent.putStringArrayListExtra("sonuclar", sonuclarListesi); // Listeyi gönder
            startActivity(intent);

            // Bu aktiviteye geri dönüldüğünde oyunun sıfırlanması için
            // resetGame(); // onResume içinde yapmak daha mantıklı olabilir


        } else {
            // --- YANLIŞ CEVAP ---
            Toast.makeText(this, "Yanlış Cevap!", Toast.LENGTH_SHORT).show();
            etIlAdi.setText(""); // EditText'i temizle
        }
    }

    // İl Plaka Eşleşmelerini Hazırlayan Metod
    private void prepareIlPlakaMap() {
        ilPlakaMap = new HashMap<>();
        ilPlakaMap.put(1, "adana");
        ilPlakaMap.put(2, "adıyaman");
        ilPlakaMap.put(3, "afyonkarahisar");
        ilPlakaMap.put(4, "ağrı");
        ilPlakaMap.put(5, "amasya");
        ilPlakaMap.put(6, "ankara");
        ilPlakaMap.put(7, "antalya");
        ilPlakaMap.put(8, "artvin");
        ilPlakaMap.put(9, "aydın");
        ilPlakaMap.put(10, "balıkesir");
        ilPlakaMap.put(11, "bilecik");
        ilPlakaMap.put(12, "bingöl");
        ilPlakaMap.put(13, "bitlis");
        ilPlakaMap.put(14, "bolu");
        ilPlakaMap.put(15, "burdur");
        ilPlakaMap.put(16, "bursa");
        ilPlakaMap.put(17, "çanakkale");
        ilPlakaMap.put(18, "çankırı");
        ilPlakaMap.put(19, "çorum");
        ilPlakaMap.put(20, "denizli");
        ilPlakaMap.put(21, "diyarbakır");
        ilPlakaMap.put(22, "edirne");
        ilPlakaMap.put(23, "elazığ");
        ilPlakaMap.put(24, "erzincan");
        ilPlakaMap.put(25, "erzurum");
        ilPlakaMap.put(26, "eskişehir");
        ilPlakaMap.put(27, "gaziantep");
        ilPlakaMap.put(28, "giresun");
        ilPlakaMap.put(29, "gümüşhane");
        ilPlakaMap.put(30, "hakkari");
        ilPlakaMap.put(31, "hatay");
        ilPlakaMap.put(32, "ısparta");
        ilPlakaMap.put(33, "mersin");
        ilPlakaMap.put(34, "istanbul");
        ilPlakaMap.put(35, "izmir");
        ilPlakaMap.put(36, "kars");
        ilPlakaMap.put(37, "kastamonu");
        ilPlakaMap.put(38, "kayseri");
        ilPlakaMap.put(39, "kırklareli");
        ilPlakaMap.put(40, "kırşehir");
        ilPlakaMap.put(41, "kocaeli");
        ilPlakaMap.put(42, "konya");
        ilPlakaMap.put(43, "kütahya");
        ilPlakaMap.put(44, "malatya");
        ilPlakaMap.put(45, "manisa");
        ilPlakaMap.put(46, "kahramanmaraş");
        ilPlakaMap.put(47, "mardin");
        ilPlakaMap.put(48, "muğla");
        ilPlakaMap.put(49, "muş");
        ilPlakaMap.put(50, "nevşehir");
        ilPlakaMap.put(51, "niğde");
        ilPlakaMap.put(52, "ordu");
        ilPlakaMap.put(53, "rize");
        ilPlakaMap.put(54, "sakarya");
        ilPlakaMap.put(55, "samsun");
        ilPlakaMap.put(56, "siirt");
        ilPlakaMap.put(57, "sinop");
        ilPlakaMap.put(58, "sivas");
        ilPlakaMap.put(59, "tekirdağ");
        ilPlakaMap.put(60, "tokat");
        ilPlakaMap.put(61, "trabzon");
        ilPlakaMap.put(62, "tunceli");
        ilPlakaMap.put(63, "şanlıurfa");
        ilPlakaMap.put(64, "uşak");
        ilPlakaMap.put(65, "van");
        ilPlakaMap.put(66, "yozgat");
        ilPlakaMap.put(67, "zonguldak");
        ilPlakaMap.put(68, "aksaray");
        ilPlakaMap.put(69, "bayburt");
        ilPlakaMap.put(70, "karaman");
        ilPlakaMap.put(71, "kırıkkale");
        ilPlakaMap.put(72, "batman");
        ilPlakaMap.put(73, "şırnak");
        ilPlakaMap.put(74, "bartın");
        ilPlakaMap.put(75, "ardahan");
        ilPlakaMap.put(76, "ığdır");
        ilPlakaMap.put(77, "yalova");
        ilPlakaMap.put(78, "karabük");
        ilPlakaMap.put(79, "kilis");
        ilPlakaMap.put(80, "osmaniye");
        ilPlakaMap.put(81, "düzce");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ResultsActivity'den geri dönüldüğünde veya uygulama açıldığında oyunu sıfırla
        // Eğer oyunun kaldığı yerden devam etmesi istenmiyorsa bu idealdir.
        resetGame();

        // Eğer ResultsActivity'den güncel listeyi almak gerekiyorsa (örneğin oyun bitmediyse)
        // Bu senaryoda, ResultsActivity'den geri dönüldüğünde listenin güncel halini
        // Intent ile geri göndermek ve burada almak gerekir. Şimdilik sıfırlama yeterli.
        // Intent intent = getIntent();
        // if (intent != null && intent.hasExtra("guncelSonuclar")) {
        //     sonuclarListesi = intent.getStringArrayListExtra("guncelSonuclar");
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Aktivite arkaplana düştüğünde sayacı durdurmak iyi bir pratik olabilir
        // (isteğe bağlı, oyunun devam etmesi isteniyorsa durdurulmaz)
        // stopTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Handler'dan kalan görevleri temizle (hafıza sızıntısını önler)
        timerHandler.removeCallbacks(timerRunnable);
    }
}