using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace WindowsFormsApp1
{
    public partial class Form1 : Form
    {
        List<Button> butonlar = new List<Button>();
        Timer oyunTimer;
        Random rand = new Random();
        int puan = 0;
        int saniye = 0; 

        public Form1()
        {
            InitializeComponent();
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            Label lblDurum = new Label();
            lblDurum.Name = "lblDurum";
            lblDurum.AutoSize = true;
            lblDurum.Location = new Point(10, 10);
            lblDurum.Font = new Font("Segoe UI", 10, FontStyle.Bold);
            lblDurum.Text = $"Puan: {puan}   Süre: {saniye}s";
            this.Controls.Add(lblDurum);

            Button btn = new Button();
            btn.Name = "ButonTek";
            btn.Text = "1";
            btn.Size = new Size(80, 60);
            btn.Font = new Font("Segoe UI", 18, FontStyle.Bold);
            btn.Click += Buton_Click;
            this.Controls.Add(btn);
            butonlar.Add(btn);

            YenidenKonumlandirVeDegerVer();

            oyunTimer = new Timer();
            oyunTimer.Interval = 1200;
            oyunTimer.Tick += (s, ev) =>
            {
                saniye++;
                YenidenKonumlandirVeDegerVer();

                var lbl = this.Controls.Find("lblDurum", false).FirstOrDefault() as Label;
                if (lbl != null) lbl.Text = $"Puan: {puan}   Süre: {saniye}s";

                if (puan > 100)
                {
                    oyunTimer.Stop();
                    foreach (var b in butonlar) b.Enabled = false;
                    MessageBox.Show($"Oyun bitti! Puanınız: {puan}", "Oyun Sonu", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
            };

            oyunTimer.Start();
        }

        private void Buton_Click(object sender, EventArgs e)
        {
            var btn = sender as Button;
            if (btn == null) return;

            int deger;
            if (!int.TryParse(btn.Tag?.ToString(), out deger))
            {
                int.TryParse(btn.Text, out deger);
            }

            if (btn.ForeColor == Color.Red)
            {
                puan += deger;
            }
            else
            {
                puan -= deger;
            }

            var lbl = this.Controls.Find("lblDurum", false).FirstOrDefault() as Label;
            if (lbl != null) lbl.Text = $"Puan: {puan}   Süre: {saniye}s";

            RastgeleDegerVeKonumAta(btn);

            if (puan > 100)
            {
                oyunTimer.Stop();
                foreach (var b in butonlar) b.Enabled = false;
                MessageBox.Show($"Oyun bitti! Puanınız: {puan}", "Oyun Sonu", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
        }

        private void YenidenKonumlandirVeDegerVer()
        {
            foreach (var btn in butonlar)
            {
                RastgeleDegerVeKonumAta(btn);
            }
        }                       

        private void RastgeleDegerVeKonumAta(Button btn)
        {
            int deger = rand.Next(1, 10);
            btn.Text = deger.ToString();
            btn.Tag = deger; 

            bool kirim = rand.NextDouble() < 0.5;
            btn.ForeColor = kirim ? Color.Red : Color.Black;

            int maxX = Math.Max(0, this.ClientSize.Width - btn.Width - 10);
            int maxY = Math.Max(30, this.ClientSize.Height - btn.Height - 10); 
            int x = rand.Next(10, maxX + 1);
            int y = rand.Next(30, maxY + 1);
            btn.Location = new Point(x, y);

            btn.Visible = true;
        }
    }
}
