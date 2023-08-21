package com.test.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.test.myapplication.ui.theme.SentryandroidgradleplugincompositebuildTheme
import io.test.compose.CompowsableFunction

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            Greeting("Android")

//            SentryandroidgradleplugincompositebuildTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Greeting("Android")
//                }
//            }
        }
    }
}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TestCompose() {
//
//    var username by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var rememberMe by remember { mutableStateOf(false) }
//
//    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//        TextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("Username") },
//            colors = TextFieldDefaults.outlinedTextFieldColors(),
////        modifier = Modifier.testTag(TEXT_FIELD_TEST_TAG)
////             Modifier.semantics {
////          this.contentDescription = "$TEXT_FIELD_TEST_TAG-content-desc" }
//        )
//        TextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            colors = TextFieldDefaults.outlinedTextFieldColors(),
//            visualTransformation = PasswordVisualTransformation()
//        )
//    }
//
//    Row(verticalAlignment = Alignment.CenterVertically) {
//        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
//        Spacer(Modifier.width(8.dp))
//        Text("Remember me")
//    }
//
//    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//        TextButton(onClick = {}) {
//            Text("SIGN IN")
//        }
//        TextButton(onClick = {}) {
//            Text("FORGOT PASSWORD")
//        }
//    }
//
//}

@Composable
fun ModifierAsParam(modifier : Modifier = Modifier.Companion) {
    CompowsableFunction(
        // expected:
        // val sentryModifier = Modifier.sentryTag("ModifierAsParam")
        // val modifier = sentryModifier.then(modifier.fillMaxSize().padding(8.dp))
        modifier = modifier.fillMaxSize().padding(8.dp),
        text = "ModifierAsParam"
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello!",
//        modifier = modifier
    )
}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    SentryandroidgradleplugincompositebuildTheme {
//        Greeting("Android")
//    }
//}
