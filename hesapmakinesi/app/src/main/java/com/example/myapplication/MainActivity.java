package com.example.myapplication;



import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText display;
    private String currentNumber = "";
    private String operation = "";
    private double firstNumber = 0;
    private boolean isNewOperation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);

        // Number buttons
        setNumberButtonClick(R.id.btn0, "0");
        setNumberButtonClick(R.id.btn1, "1");
        setNumberButtonClick(R.id.btn2, "2");
        setNumberButtonClick(R.id.btn3, "3");
        setNumberButtonClick(R.id.btn4, "4");
        setNumberButtonClick(R.id.btn5, "5");
        setNumberButtonClick(R.id.btn6, "6");
        setNumberButtonClick(R.id.btn7, "7");
        setNumberButtonClick(R.id.btn8, "8");
        setNumberButtonClick(R.id.btn9, "9");

        // Operation buttons
        setOperationButtonClick(R.id.btnPlus, "+");
        setOperationButtonClick(R.id.btnMinus, "-");
        setOperationButtonClick(R.id.btnMultiply, "*");
        setOperationButtonClick(R.id.btnDivide, "/");

        // Clear button
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            currentNumber = "";
            operation = "";
            firstNumber = 0;
            isNewOperation = true;
            display.setText("0");
        });

        // Equals button
        findViewById(R.id.btnEquals).setOnClickListener(v -> calculate());
    }

    private void setNumberButtonClick(int buttonId, String number) {
        findViewById(buttonId).setOnClickListener(v -> {
            if (isNewOperation) {
                display.setText("");
                isNewOperation = false;
            }
            currentNumber += number;
            display.setText(currentNumber);
        });
    }

    private void setOperationButtonClick(int buttonId, String op) {
        findViewById(buttonId).setOnClickListener(v -> {
            if (!currentNumber.isEmpty()) {
                if (!operation.isEmpty()) {
                    calculate();
                }
                firstNumber = Double.parseDouble(currentNumber);
                operation = op;
                currentNumber = "";
                display.setText(String.valueOf(firstNumber));
            }
        });
    }

    private void calculate() {
        if (!currentNumber.isEmpty() && !operation.isEmpty()) {
            double secondNumber = Double.parseDouble(currentNumber);
            double result = 0;

            switch (operation) {
                case "+":
                    result = firstNumber + secondNumber;
                    break;
                case "-":
                    result = firstNumber - secondNumber;
                    break;
                case "*":
                    result = firstNumber * secondNumber;
                    break;
                case "/":
                    if (secondNumber != 0) {
                        result = firstNumber / secondNumber;
                    } else {
                        display.setText("Error");
                        return;
                    }
                    break;
            }

            display.setText(String.valueOf(result));
            currentNumber = String.valueOf(result);
            operation = "";
            isNewOperation = true;
        }
    }
}