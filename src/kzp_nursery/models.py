"""Об'єктна модель розсадника рослин (варіант 12): базовий клас, підтипи, Protocol і dataclass."""

from dataclasses import dataclass
import math
from typing import Protocol, runtime_checkable


@runtime_checkable
class CareScored(Protocol):
    """Описує спільний протокол рослини з властивостями стану та оцінкою потреби в догляді."""

    @property
    def species(self) -> str:
        """Повертає вид рослини."""
        ...

    @property
    def age(self) -> int:
        """Повертає вік рослини у роках."""
        ...

    @property
    def height(self) -> float:
        """Повертає висоту рослини у сантиметрах."""
        ...

    @property
    def price(self) -> float:
        """Повертає ціну рослини у гривнях."""
        ...

    @property
    def watering_days(self) -> int:
        """Повертає інтервал поливу у днях."""
        ...

    def care_score(self) -> float:
        """Повертає числовий показник потреби рослини в догляді."""
        ...


class Plant:
    """Базова сутність рослини в розсаднику з перевіреним інкапсульованим станом."""

    def __init__(
        self,
        species: str,
        age: int,
        height: float,
        price: float,
        watering_days: int,
    ) -> None:
        """Створює рослину та перевіряє початкові значення через властивості."""
        self.species = species
        self.age = age
        self.height = height
        self.price = price
        self.watering_days = watering_days

    @property
    def species(self) -> str:
        """Повертає назву виду рослини."""
        return self._species

    @species.setter
    def species(self, value: str) -> None:
        """Встановлює непорожню назву виду рослини."""
        # Внутрішнє поле _species не викликає setter повторно й уникає рекурсії.
        if not isinstance(value, str) or not value.strip():
            raise ValueError("Вид рослини не може бути порожнім")
        self._species = value.strip()

    @property
    def age(self) -> int:
        """Повертає вік рослини у роках."""
        return self._age

    @age.setter
    def age(self, value: int) -> None:
        """Встановлює додатний цілий вік рослини."""
        if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
            raise ValueError("Вік рослини має бути додатним цілим числом")
        self._age = value

    @property
    def height(self) -> float:
        """Повертає висоту рослини у сантиметрах."""
        return self._height

    @height.setter
    def height(self, value: float) -> None:
        """Встановлює додатну скінченну висоту рослини."""
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            raise ValueError("Висота рослини має бути числом")
        numeric = float(value)
        if not math.isfinite(numeric) or numeric <= 0:
            raise ValueError("Висота рослини має бути додатним скінченним числом")
        self._height = numeric

    @property
    def price(self) -> float:
        """Повертає ціну рослини у гривнях."""
        return self._price

    @price.setter
    def price(self, value: float) -> None:
        """Встановлює невід'ємну скінченну ціну рослини."""
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            raise ValueError("Ціна рослини має бути числом")
        numeric = float(value)
        if not math.isfinite(numeric) or numeric < 0:
            raise ValueError("Ціна рослини не може бути від'ємною")
        self._price = numeric

    @property
    def watering_days(self) -> int:
        """Повертає інтервал поливу рослини у днях."""
        return self._watering_days

    @watering_days.setter
    def watering_days(self, value: int) -> None:
        """Встановлює додатний цілий інтервал поливу у днях."""
        if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
            raise ValueError("Інтервал поливу має бути додатним цілим числом")
        self._watering_days = value

    def needs_frequent_watering(self, max_days: int = 2) -> bool:
        """Перевіряє, чи рослина потребує частого поливу (не рідше ніж раз на max_days днів)."""
        if isinstance(max_days, bool) or not isinstance(max_days, int) or max_days <= 0:
            raise ValueError("Максимальний інтервал поливу має бути додатним")
        return self.watering_days <= max_days

    def care_score(self) -> float:
        """Оголошує спільну операцію оцінювання потреби в догляді для підтипів."""
        raise NotImplementedError("Підтипи Plant мають реалізувати метод care_score()")

    def __str__(self) -> str:
        """Повертає стисле людиночитане подання рослини для звіту."""
        return f"{self.species} ({self.height:.2f} см, вік {self.age} р., {self.price:.2f} грн)"

    def __repr__(self) -> str:
        """Повертає діагностичне подання об'єкта рослини."""
        return (
            f"{type(self).__name__}(species={self.species!r}, age={self.age!r}, "
            f"height={self.height!r}, price={self.price!r}, "
            f"watering_days={self.watering_days!r})"
        )

    def __eq__(self, other: object) -> bool:
        """Порівнює рослини одного конкретного підтипу за повним станом."""
        if not isinstance(other, Plant):
            return NotImplemented
        return type(self) is type(other) and self.__dict__ == other.__dict__

    def __lt__(self, other: object) -> bool:
        """Порівнює рослини за висотою, видом, віком, ціною та інтервалом поливу."""
        if not isinstance(other, Plant):
            return NotImplemented
        return (
            self.height,
            self.species,
            self.age,
            self.price,
            self.watering_days,
        ) < (
            other.height,
            other.species,
            other.age,
            other.price,
            other.watering_days,
        )


class Tree(Plant):
    """Підтип деревних рослин, для яких потреба в догляді залежить передусім від висоти крони."""

    def __init__(
        self,
        species: str,
        age: int,
        height: float,
        price: float,
        watering_days: int,
    ) -> None:
        """Створює деревну рослину через ініціалізатор базового класу Plant."""
        super().__init__(species, age, height, price, watering_days)

    def care_score(self) -> float:
        """Обчислює потребу дерева в догляді з акцентом на висоту крони та вік."""
        return round(self.height * 0.5 + self.age * 2.0 + (30.0 / self.watering_days), 2)


class Flower(Plant):
    """Підтип квіткових і трав'янистих рослин, де догляд визначається інтенсивністю поливу."""

    def __init__(
        self,
        species: str,
        age: int,
        height: float,
        price: float,
        watering_days: int,
    ) -> None:
        """Створює квіткову рослину через ініціалізатор базового класу Plant."""
        super().__init__(species, age, height, price, watering_days)

    def care_score(self) -> float:
        """Обчислює потребу квітки в догляді з акцентом на частоту поливу."""
        return round((120.0 / self.watering_days) + self.height * 0.2 + self.age * 1.5, 2)


@dataclass(frozen=True)
class CareSummary:
    """Зберігає незмінний підсумок потреби рослин у догляді (варіант 12)."""

    count: int
    total_care_score: float
    average_care_score: float
    highest_care_species: str | None = None
    highest_care_score: float = 0.0


@dataclass(frozen=True)
class ReportSummary:
    """Зберігає незмінний повний підсумок аналізу розсадника рослин (варіант 12)."""

    count: int
    frequent_watering_count: int
    average_price: float
    tallest_plant: Plant | None
    species: tuple[str, ...]
    average_price_by_species: tuple[tuple[str, float], ...]
    care: CareSummary
    title: str = "Звіт розсадника рослин (варіант 12)"
