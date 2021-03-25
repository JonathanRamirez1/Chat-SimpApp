package com.jonathan.loginfuturo

sealed class BaseCommand {
    class Error(val errorString: String): BaseCommand()
    class Success(val toastMessage: String?): BaseCommand()
}