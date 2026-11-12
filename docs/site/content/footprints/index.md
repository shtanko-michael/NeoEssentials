<p class="cta-row"><a class="cta" href="https://modrinth.com/mod/farmstead-footprints">⬇ Modrinth</a> <a class="cta cta-soft" href="https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints">CurseForge</a> <a class="cta cta-soft" href="https://github.com/shtanko-michael/farmstead-footprints">GitHub</a></p>

**Реалистичные следы игроков и мобов.** Пройдите по снегу, песку или грязи — шаги останутся позади: вдавленные в землю, освещённые так же, как блоки вокруг, и постепенно исчезающие. Волки оставляют отпечатки лап, лошади — копыт, куры — трёхпалые царапины. Мод клиентский: поставили — и он работает на любом сервере, включая ванильный.

## Как это выглядит

| | |
| --- | --- |
| ![A trail toward the mountains](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/snow_hero.gif) | ![A wolf pack in fresh snow](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/wolves.gif) |
| Ваша тропа тянется следом | У каждого моба свои следы |
| ![Prints crumbling away](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/crumble.gif) | ![Detailed and pixel style](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/styles.gif) |
| Старые следы осыпаются от краёв | Два вида: детальный и пиксельный |
| ![A boat's foam wake](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/wake.gif) | ![Rings where a villager wades](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/ripples.gif) |
| За лодкой остаётся пена | Круги там, где идут вброд |
| ![Try it from the settings](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/tryit.gif) | ![Searching blocks in the settings](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/picker.gif) |
| «Показать» печатает следы там, куда вы смотрите | Поиск блоков и мобов |

Больше картинок в [docs/media/gallery](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gallery).

## Возможности

- **Два вида.** *Детальный* (по умолчанию): затенённая впадина и освещённый край, цвет от грунта — синие тени на снегу, тёмный песок, мокрый блеск на грязи. *Пиксельный*: вдавлены сами пиксели текстуры блока, северо-западная стенка в тени, как в ванильных текстурах; совпадает с повёрнутыми текстурами травы и цветом биома.
- **Игроки и 47 мобов**, у каждого своя стопа: ботинки или босые ноги, лапы, кошачьи лапы, копыта, раздвоенные копыта, птичьи лапы, медвежьи лапы, верблюжьи ноги, огромные ступни големов, разорителей и стражей. Детёныши и увеличенные мобы (атрибут scale, Pehkui, Random Mob Sizes) оставляют следы по размеру.
- **Модовые мобы** получают ноги из своей модели: кубы, касающиеся земли, — это ступни; две дают двуногого, больше — четыре, шесть или восемь ног на настоящих местах. Мобы без ванильных частей модели (GeckoLib) — по хитбоксу.
- **Настоящая походка.** Двуногие ступают левой и правой по очереди; четвероногие ставят заднюю ногу в след передней той же стороны; кролики и лягушки приземляются группами по четыре.
- **Любой мягкий грунт.** Следы оставляет любой блок мягче `surfaces.hardnessGate`, модовые тоже; списками по id или #тегу блоки добавляются и исключаются. У снега, песка, грязи, травы, гравия, земли, глины и блоков душ свой цвет и срок. Тонкие слои снега тоже.
- **Следы стареют.** Во второй половине жизни осыпаются от краёв (`effects.decay = FADE` — просто бледнеют). Дождь и снегопад стирают следы под открытым небом быстрее. След исчезает, если блок под ним сломали или заменили.
- **Вода.** За лодкой пенный след и брызги, где идут вброд — круги, за пловцами пузыри, мокрые ноги оставляют влажные следы на камне и досках и брызги. Быстрые вагонетки искрят на рельсах.
- **Свои следы** из PNG или ресурспаков — см. ниже.
- **Быстро.** Один вызов отрисовки на кадр из геометрии, посчитанной при появлении следа, только то, что видит камера.
- **Шейдеры.** Работает с Sodium и Iris (проверено с Complementary Unbound).
- **Настройки для всех:** вкладки, поиск блоков и мобов, вкладка «О моде» со статусом обновлений, «Показать» — текущие настройки прямо на земле, пресеты одной строкой, конфиг с комментариями, `/footprints reload`.
- **12 языков:** английский, русский, китайский (упрощённый), португальский (Бразилия), испанский, немецкий, французский, польский, украинский, японский, корейский, турецкий.
