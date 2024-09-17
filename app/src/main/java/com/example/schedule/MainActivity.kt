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
            val periods = mutableListOf<String>()

            for (row in rows) {


                val cells = row.select("td")

                // Пропускаем строки, содержащие только &nbsp;
                if (cells.isNotEmpty() && cells.all { it.text().trim() == "&nbsp;" }) {
                    continue
                }

                // Пропускаем строки с классом "nul" и пустым содержимым
                if (cells.size == 1 && cells[0].hasClass("nul") && cells[0].text().trim().isEmpty()) {
                    continue
                }

                // Пропускаем строки, которые содержат только цифру и пустое содержимое
                if (cells.size == 2 && cells[0].hasClass("hd") && cells[1].text().trim().isEmpty()) {
                    continue
                }

                // Обработка строки-разделителя для конца дня
                if (cells.size == 1 && cells[0].hasClass("hd0")) {
                    if (periods.isNotEmpty()) {
                        if (currentDate != null) {
                            scheduleItems.add("Расписание на $currentDate:")
                        }
                        scheduleItems.addAll(periods)
                        periods.clear()
                        scheduleItems.add("") // Добавляем пустую строку между днями
                    }
                    continue
                }

                // Обработка строки с датой и днем недели
                if (cells.size > 1 && cells[0].text().matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))) {
                    if (periods.isNotEmpty()) {
                        if (currentDate != null) {
                            scheduleItems.add("Расписание на $currentDate:")
                        }
                        scheduleItems.addAll(periods)
                        periods.clear()
                    }
                    currentDate = cells[0].text().trim()
                    // Добавляем дату и день недели как отдельную строку
                    scheduleItems.add("$currentDate")
                    scheduleItems.add("") // Добавляем пустую строку после даты
                    continue
                }

                // Обработка строки с данными пары
                if (cells.size >= 2 && cells[0].hasClass("hd")) {
                    val period = cells[0].text().trim()
                    val description = cells.getOrNull(1)?.text()?.trim() ?: ""
                    val additionalInfo = cells.getOrNull(2)?.text()?.trim() ?: ""

                    val scheduleText = buildString {
                        append("$period. $description")
                        if (additionalInfo.isNotEmpty()) {
                            append(" $additionalInfo")
                        }
                    }.trim()

                    if (scheduleText.isNotEmpty()) {
                        if (cells.size == 4) {  // Обработка пар с подгруппами
                            val secondDescription = cells.getOrNull(3)?.text()?.trim() ?: ""
                            if (secondDescription.isNotEmpty()) {
                                val updatedScheduleText = "$scheduleText / $secondDescription"
                                periods.add(updatedScheduleText)
                            }
                        } else {
                            periods.add(scheduleText)
                        }
                    }
                }
            }

            // Добавляем оставшиеся элементы
            if (periods.isNotEmpty()) {
                if (currentDate != null) {
                    scheduleItems.add("Расписание на $currentDate:")
                }
                scheduleItems.addAll(periods)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            scheduleItems.add("Ошибка при загрузке расписания")
        }
        return scheduleItems
    }
















}