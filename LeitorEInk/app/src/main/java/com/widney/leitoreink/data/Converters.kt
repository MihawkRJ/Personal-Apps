/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBookFormat(format: BookFormat): String = format.name

    @TypeConverter
    fun toBookFormat(value: String): BookFormat = BookFormat.valueOf(value)
}
