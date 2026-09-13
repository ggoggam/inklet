package dev.ggoggam.inklet.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.material3.InkletTextField
import dev.ggoggam.inklet.sample.icons.Heart
import dev.ggoggam.inklet.sample.icons.Lucide
import dev.ggoggam.inklet.sample.icons.X

@Composable
internal fun TextFieldExamples() {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("A slow morning, with nowhere to hurry.") }
    var guests by remember { mutableStateOf("0") }
    var secret by remember { mutableStateOf("a little surprise") }
    val guestError = (guests.toIntOrNull() ?: 0) < 1
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Words for later", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        InkletTextField(
            title,
            { title = it },
            Modifier.fillMaxWidth(),
            label = { Text("Our next little adventure") },
            placeholder = { Text("Somewhere with a sunny window") },
            leadingIcon = { Icon(Lucide.Heart, null, Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { title = "" }, enabled = title.isNotEmpty()) {
                    Icon(Lucide.X, "Clear adventure", Modifier.size(20.dp))
                }
            },
            supportingText = { Text("A few words are enough.") },
            seed = 210,
        )
        InkletTextField(
            note,
            { note = it },
            Modifier.fillMaxWidth(),
            label = { Text("A note for us") },
            singleLine = false,
            minLines = 3,
            maxLines = 5,
            supportingText = { Text("${note.length} characters of possibility") },
            seed = 211,
        )
        InkletTextField(
            guests,
            { guests = it },
            Modifier.fillMaxWidth(),
            label = { Text("At our table") },
            suffix = { Text("guests") },
            isError = guestError,
            errorMessage = "Invite at least one guest",
            supportingText = { Text(if (guestError) "Invite at least one guest" else "There's room for everyone.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            seed = 212,
        )
        InkletTextField(
            secret,
            { secret = it },
            Modifier.fillMaxWidth(),
            label = { Text("Our secret phrase") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            seed = 213,
        )
        InkletTextField(
            "Saturday, just for us",
            {},
            Modifier.fillMaxWidth(),
            label = { Text("Saved plan") },
            readOnly = true,
            supportingText = { Text("A keepsake to select and copy.") },
            seed = 214,
        )
        InkletTextField(
            "When the snow arrives",
            {},
            Modifier.fillMaxWidth(),
            label = { Text("Winter plans") },
            enabled = false,
            supportingText = { Text("We'll come back to this later.") },
            seed = 215,
        )
    }
}
