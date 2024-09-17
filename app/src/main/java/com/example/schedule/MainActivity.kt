package com.example.schedule

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.schedule_list)

        // Edge to Edge настройка
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Запуск парсинга расписания в фоновом потоке
        CoroutineScope(Dispatchers.IO).launch {
            val scheduleItems = fetchSchedule()
            CoroutineScope(Dispatchers.Main).launch {
                // Настраиваем адаптер
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, scheduleItems)
                listView.adapter = adapter
            }
        }
    }

    private fun fetchSchedule(): List<String> {
        val scheduleItems = mutableListOf<String>()

        try {
            // URL расписания
            val url = "https://www.chtotib.ru/schedule_gl/cg60.htm"

            // Загружаем страницу
            val document = Jsoup.connect(url).get()

            // Получаем все строки таблицы
            val rows = document.select("tr")

            var currentDate: String? = null
            var previousDigit: String? = null
            var lineCounter = 0

            for (row in rows) {
                if (lineCounter < 13) {
                    // Пропускаем первые 13 строк
                    lineCounter++
                    continue
                }

                val cells = row.select("td")

                // Пропускаем строки, содержащие только &nbsp;
                if (cells.isNotEmpty() && cells.all { it.text().trim() == "&nbsp;" }) {
                    if (previousDigit != null) {
                        // Не добавляем цифру, если после нее идет пустая строка
                        previousDigit = null
                    }
                    continue
                }

                // Пропускаем строки с классом "nul" и пустым содержимым
                if (cells.size == 1 && cells[0].hasClass("nul") && cells[0].text().trim().isEmpty()) {
                    if (previousDigit != null) {
                        // Не добавляем цифру, если после нее идет строка с классом "nul"
                        previousDigit = null
                    }
                    continue
                }

                // Обработка строки-разделителя для конца дня
                if (cells.size == 1 && cells[0].hasClass("hd0")) {
                    if (currentDate != null) {
                        scheduleItems.add("Расписание на $currentDate:")
                    }
                    scheduleItems.add("") // Добавляем пустую строку между днями
                    previousDigit = null // Сбрасываем предыдущую цифру
                    continue
                }

                // Обработка строки с датой и днем недели
                if (cells.size > 1 && cells[0].text().matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))) {
                    if (currentDate != null) {
                        if (previousDigit != null) {
                            scheduleItems.add(previousDigit) // Добавляем запомненную цифру перед новой датой
                            previousDigit = null
                        }
                        scheduleItems.add("Расписание на $currentDate:")
                    }
                    currentDate = cells[0].text().trim()
                    scheduleItems.add("$currentDate")
                    scheduleItems.add("") // Добавляем пустую строку после даты
                    continue
                }

                // Обработка строки с данными пары
                for (cell in cells) {
                    val cellText = cell.text().trim()
                    if (cellText.isNotEmpty() && cellText != "&nbsp;") {
                        if (cellText.matches(Regex("\\d+"))) {
                            if (previousDigit != null) {
                                // Не добавляем предыдущую цифру, если после нее идет пустая строка
                                previousDigit = cellText
                            } else {
                                // Запоминаем текущую цифру
                                previousDigit = cellText
                            }
                        } else {
                            if (previousDigit != null) {
                                scheduleItems.add(previousDigit) // Добавляем запомненную цифру перед текстом
                                previousDigit = null
                            }
                            scheduleItems.add(cellText)
                        }
                    }
                }
            }

            // Добавляем оставшиеся элементы
            if (currentDate != null) {
                if (previousDigit != null) {
                    scheduleItems.add(previousDigit) // Добавляем запомненную цифру перед концом
                }
                scheduleItems.add("Расписание на $currentDate:")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            scheduleItems.add("Ошибка при загрузке расписания")
        }
        return scheduleItems
    }
















}