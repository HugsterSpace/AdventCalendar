package space.hugster.calendar.utils.menu.event

import space.hugster.calendar.utils.menu.Menu

class MenuOpenEvent(val menu: Menu) : BaseEvent()
class MenuUpdateEvent(val menu: Menu) : BaseEvent()
