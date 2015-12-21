package com.hsfb.ullauri.finalproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;

public class EnterBudget extends AppCompatActivity {
    public static final String EXTRA_ENTEREDBUDGET = "com.hsfb.ullauri.finalproject.BUDGET";

    private Button enterButton;
    private String budget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_budget);

        // gets enterButton
        enterButton = (Button) findViewById(R.id.enterButton);
    }

    // passes the value entered to ShoppingCart for budget when button is clicked (onClick: )
    public void shoppingCart(View view){
        Intent shoppingCart = new Intent(this, ShoppingCart.class);
        EditText budgetEditText = (EditText) findViewById(R.id.budgetEditText);
        String budget = budgetEditText.getText().toString();
        shoppingCart.putExtra(EXTRA_ENTEREDBUDGET, budget);
        startActivity(shoppingCart);
    }

}
