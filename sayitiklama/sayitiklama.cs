using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;
using System.Diagnostics;

namespace sayıtiklama
{
    public partial class Form1 : Form
    {
        private readonly Random _rand = new Random();
        private Stopwatch _sw;

        public Form1()
        {
            InitializeComponent();
            this.button1.Click += Button1_Click;
            this.button2.Click += Button2_Click;
            this.timer1.Interval = 1000;
            this.timer1.Tick += Timer1_Tick;
            label1.Text = "";
        }

        private void Form1_Load(object sender, EventArgs e)
        {
        }

        private void Shuffle<T>(IList<T> list)
        {
            for (int i = list.Count - 1; i > 0; i--)
            {
                int j = _rand.Next(i + 1);
                T tmp = list[i];
                list[i] = list[j];
                list[j] = tmp;
            }
        }

        private void Button1_Click(object sender, EventArgs e)
        {
            listBox1.Items.Clear();
            panel1.Controls.Clear();

            const int count = 12;
            const int minValue = 0;
            const int maxValue = 99;

            var numbers = new List<int>();
            for (int i = 0; i < count; i++)
            {
                numbers.Add(_rand.Next(minValue, maxValue + 1));
            }

            Shuffle(numbers);

            var btnSize = new Size(60, 30);
            const int padding = 8;
            const int spacing = 8;
            int cols = Math.Max(1, (panel1.ClientSize.Width - padding * 2 + spacing) / (btnSize.Width + spacing));

            for (int i = 0; i < numbers.Count; i++)
            {
                int n = numbers[i];
                var btn = new Button
                {
                    Size = btnSize,
                    Text = n.ToString(),
                    Tag = n,
                    BackColor = SystemColors.ControlLight,
                };

                int col = i % cols;
                int row = i / cols;
                btn.Left = padding + col * (btnSize.Width + spacing);
                btn.Top = padding + row * (btnSize.Height + spacing);

                btn.Click += NumberButton_Click;
            
                panel1.Controls.Add(btn);
            }

            _sw = Stopwatch.StartNew();
            timer1.Start();
        }

        private void NumberButton_Click(object sender, EventArgs e)
        {
            var btn = sender as Button;
            if (btn == null) return;
            if (!(btn.Tag is int)) return;
            int n = (int)btn.Tag;

            if (n % 2 != 0)
            {
                btn.BackColor = Color.LightCoral;
                return;
            }

            var availableEvenValues = panel1.Controls
                .OfType<Button>()
                .Where(b => b.Tag is int && b.Enabled && ((int)b.Tag) % 2 == 0)
                .Select(b => (int)b.Tag);

            if (!availableEvenValues.Any())
            {
                return;
            }

            int minEven = availableEvenValues.Min();

            if (n == minEven)
            {
                if (!listBox1.Items.Contains(n))
                {
                    listBox1.Items.Add(n);
                }
                btn.Enabled = false;
                btn.BackColor = Color.LightGreen;
            }
            else
            {
                btn.BackColor = Color.LightCoral;
            }
        }

        private void Timer1_Tick(object sender, EventArgs e)
        {
            if (_sw == null) return;
            label1.Text = _sw.Elapsed.ToString(@"mm\:ss");
        }

        private void Button2_Click(object sender, EventArgs e)
        {
            timer1.Stop();
            if (_sw != null)
            {
                _sw.Stop();
            }

            panel1.Controls.Clear();
            listBox1.Items.Clear();

            var elapsedText = _sw != null ? _sw.Elapsed.ToString(@"mm\:ss") : "00:00";
            label1.Text = elapsedText;
        }
    }
}
