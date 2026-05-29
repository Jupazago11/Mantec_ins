package com.example.mantec_ins.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.AuthRepository
import com.example.mantec_ins.data.repository.RemoteCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val remoteCatalogRepository: RemoteCatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(username: String, password: String) {
        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            val result = authRepository.login(username, password)

            result.fold(
                onSuccess = { session ->
                    try {
                        if (session.roleKey != "inspector") {
                            Log.w(
                                "LOGIN",
                                "Login exitoso para rol no inspector: ${session.roleKey}. No se descarga catálogo móvil."
                            )

                            _uiState.value = LoginUiState(
                                isLoading = false,
                                loginSuccess = true,
                                errorMessage = null
                            )
                            return@fold
                        }

                        Log.d("LOGIN", "Login inspector exitoso. Iniciando syncOfflineCatalog()")

                        remoteCatalogRepository.syncOfflineCatalog()

                        Log.d("LOGIN", "syncOfflineCatalog() finalizado correctamente")

                        _uiState.value = LoginUiState(
                            isLoading = false,
                            loginSuccess = true,
                            errorMessage = null
                        )
                    } catch (e: Exception) {
                        Log.e("LOGIN", "Error sincronizando catálogo offline", e)

                        _uiState.value = LoginUiState(
                            isLoading = false,
                            loginSuccess = false,
                            errorMessage = mapCatalogError(e)
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("LOGIN", "Error de login", error)

                    _uiState.value = LoginUiState(
                        isLoading = false,
                        loginSuccess = false,
                        errorMessage = mapLoginError(error)
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun mapLoginError(error: Throwable): String {
        return when (error) {
            is SocketTimeoutException -> {
                "La conexión tardó demasiado. Intenta nuevamente."
            }

            is IOException -> {
                "Debes estar conectado a internet para iniciar sesión."
            }

            is HttpException -> {
                when (error.code()) {
                    400 -> "La solicitud de inicio de sesión no es válida."
                    401 -> "Usuario o contraseña incorrectos."
                    403 -> "Tu cuenta no tiene permiso para iniciar sesión."
                    404 -> "No se encontró el servicio de autenticación."
                    422 -> "No fue posible procesar el inicio de sesión. Verifica tus datos."
                    500 -> "El servidor presentó un error. Intenta más tarde."
                    502, 503, 504 -> "El servidor no está disponible en este momento. Intenta más tarde."
                    else -> "Ocurrió un error al iniciar sesión. Código: ${error.code()}."
                }
            }

            else -> {
                val rawMessage = error.message.orEmpty()

                when {
                    rawMessage.contains("Unable to resolve host", ignoreCase = true) ->
                        "No se pudo establecer conexión. Verifica tu internet."

                    rawMessage.contains("Failed to connect", ignoreCase = true) ->
                        "No se pudo conectar con el servidor. Verifica tu internet o intenta más tarde."

                    rawMessage.contains("timeout", ignoreCase = true) ->
                        "La conexión tardó demasiado. Intenta nuevamente."

                    else ->
                        "Ocurrió un error inesperado al iniciar sesión."
                }
            }
        }
    }

    private fun mapCatalogError(error: Throwable): String {
        return when (error) {
            is SocketTimeoutException -> {
                "Se inició sesión, pero la descarga del catálogo tardó demasiado. Intenta nuevamente."
            }

            is IOException -> {
                "Se inició sesión, pero se perdió la conexión al descargar el catálogo offline."
            }

            is HttpException -> {
                when (error.code()) {
                    401 -> "La sesión no es válida para descargar el catálogo offline."
                    403 -> "No tienes permisos para descargar el catálogo offline."
                    404 -> "No se encontró el servicio de catálogo offline."
                    422 -> "No fue posible descargar el catálogo offline con la configuración actual del inspector."
                    500 -> "El servidor presentó un error al generar el catálogo offline."
                    502, 503, 504 -> "El servicio de catálogo offline no está disponible en este momento."
                    else -> "No fue posible descargar el catálogo offline. Código: ${error.code()}."
                }
            }

            else -> {
                val rawMessage = error.message.orEmpty()

                when {
                    rawMessage.contains("Use JsonReader.setLenient(true)", ignoreCase = true) ->
                        "El servidor devolvió un catálogo inválido. Revisa la configuración del backend."

                    rawMessage.contains("Expected BEGIN_OBJECT", ignoreCase = true) ||
                            rawMessage.contains("Expected BEGIN_ARRAY", ignoreCase = true) ||
                            rawMessage.contains("MalformedJsonException", ignoreCase = true) ->
                        "El catálogo offline llegó con un formato inválido desde el servidor."

                    rawMessage.contains("IllegalStateException", ignoreCase = true) ->
                        "No fue posible procesar el catálogo offline recibido."

                    rawMessage.contains("Failed to connect", ignoreCase = true) ->
                        "Se inició sesión, pero no fue posible conectar con el servidor para descargar el catálogo."

                    else ->
                        "Se inició sesión, pero ocurrió un error al descargar el catálogo offline."
                }
            }
        }
    }
}