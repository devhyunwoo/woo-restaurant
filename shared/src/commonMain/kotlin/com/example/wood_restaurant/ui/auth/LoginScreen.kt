package com.example.wood_restaurant.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * 로그인 / 회원가입 화면. 한 화면에서 [AuthMode] 만 바꾼다 — 필드가 하나(닉네임) 차이라 화면을 둘로 나눌 이유가 없다.
 *
 * @param onLoggedIn 성공 시. 앱 첫 진입이면 메인으로, 마이 탭에서 왔으면 뒤로.
 * @param onSkip "나중에 할게요". 장소 검색은 로그인 없이도 되므로 막지 않는다.
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is AuthSideEffect.LoggedIn -> onLoggedIn()
            is AuthSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("우드 레스토랑", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = if (state.mode == AuthMode.LOGIN) "다시 만나서 반가워요" else "찜한 맛집을 어디서든 이어서 보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            AuthForm(
                state = state,
                onEmailChanged = viewModel::onEmailChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onNicknameChanged = viewModel::onNicknameChanged,
                onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
                onSubmit = viewModel::onSubmit,
            )

            state.errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = viewModel::onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (state.mode == AuthMode.LOGIN) "로그인" else "가입하고 시작하기")
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = viewModel::onToggleMode, enabled = !state.isLoading) {
                Text(
                    if (state.mode == AuthMode.LOGIN) "계정이 없으신가요? 회원가입"
                    else "이미 계정이 있으신가요? 로그인",
                )
            }
            TextButton(onClick = onSkip, enabled = !state.isLoading) {
                Text("나중에 할게요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AuthForm(
    state: AuthState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = { Text("이메일") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            singleLine = true,
            isError = state.email.isNotEmpty() && !state.isEmailValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.mode == AuthMode.SIGNUP) {
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChanged,
                label = { Text("닉네임 (2~30자)") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                isError = state.nickname.isNotEmpty() && !state.isNicknameValid,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = { Text(if (state.mode == AuthMode.SIGNUP) "비밀번호 (8자 이상)" else "비밀번호") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                // material-icons-core 에는 Visibility 아이콘이 없어서 글자로 토글한다.
                IconButton(onClick = onTogglePasswordVisibility) {
                    Text(
                        text = if (state.isPasswordVisible) "숨김" else "표시",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            singleLine = true,
            isError = state.password.isNotEmpty() && !state.isPasswordValid,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
