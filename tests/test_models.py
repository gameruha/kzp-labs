"""Поведінкові тести моделей, властивостей, @dataclass, підтипів і протоколу CareScored (ЛР №7)."""

import dataclasses

import pytest

from kzp_lab07.analysis import (
    average_care_score,
    care_summary,
    highest_care_plant,
    sort_by_care_score,
    total_care_score,
)
from kzp_lab07.models import CareScored, CareSummary, Flower, Plant, ReportSummary, Tree


def test_base_plant_properties_and_setter_validation() -> None:
    """Перевіряє коректні, нульові, граничні та помилкові значення у setter-властивостях."""
    plant = Tree(species="  Хвойні  ", age=1, height=0.5, price=0.0, watering_days=1)

    assert plant.species == "Хвойні"
    assert plant.age == 1
    assert plant.height == 0.5
    assert plant.price == 0.0
    assert plant.watering_days == 1
    assert plant.needs_frequent_watering(max_days=1) is True
    assert plant.needs_frequent_watering(max_days=2) is True

    # Перевірка зміни стану через setter для коректних і граничних значень.
    plant.species = "Листяні"
    plant.age = 5
    plant.height = 180.0
    plant.price = 0.0
    plant.watering_days = 3
    assert plant.species == "Листяні"
    assert plant.age == 5
    assert plant.height == 180.0
    assert plant.price == 0.0
    assert plant.watering_days == 3
    assert plant.needs_frequent_watering(max_days=2) is False

    # Відхилення порожнього виду, нульових/від'ємних та нечислових значень.
    with pytest.raises(ValueError, match="порожнім"):
        plant.species = "   "
    with pytest.raises(ValueError, match="додатним"):
        plant.age = 0
    with pytest.raises(ValueError, match="додатним"):
        plant.age = -2
    with pytest.raises(ValueError, match="додатним"):
        plant.age = True  # type: ignore[assignment]
    with pytest.raises(ValueError, match="додатним"):
        plant.height = 0.0
    with pytest.raises(ValueError, match="додатним"):
        plant.height = float("nan")
    with pytest.raises(ValueError, match="від'ємною"):
        plant.price = -0.01
    with pytest.raises(ValueError, match="від'ємною"):
        plant.price = float("inf")
    with pytest.raises(ValueError, match="додатним"):
        plant.watering_days = 0
    with pytest.raises(ValueError, match="додатним"):
        plant.needs_frequent_watering(0)


def test_base_plant_care_score_raises_not_implemented() -> None:
    """Перевіряє, що базовий клас Plant вимагає реалізації care_score у підтипах."""
    base = Plant("Хвойні", 4, 120.5, 450.0, 7)
    with pytest.raises(NotImplementedError):
        base.care_score()


def test_subtypes_have_different_care_scores() -> None:
    """Перевіряє різні формули care_score() у підтипах Tree та Flower за однакових параметрів."""
    tree = Tree("Декоративні", age=2, height=60.0, price=150.0, watering_days=2)
    flower = Flower("Декоративні", age=2, height=60.0, price=150.0, watering_days=2)

    assert isinstance(tree, Plant)
    assert isinstance(flower, Plant)
    assert isinstance(tree, CareScored)
    assert isinstance(flower, CareScored)
    assert tree.care_score() == 49.0
    assert flower.care_score() == 75.0
    assert tree.care_score() != flower.care_score()


def test_special_methods_str_repr_eq_and_lt() -> None:
    """Перевіряє __str__, __repr__, __eq__ (включно з несумісними типами) та впорядкування."""
    flower_a = Flower("Квіти", 2, 60.0, 150.0, 1)
    flower_b = Flower("Квіти", 2, 60.0, 150.0, 1)
    tree_same_fields = Tree("Квіти", 2, 60.0, 150.0, 1)
    taller_tree = Tree("Хвойні", 8, 300.0, 800.0, 2)

    assert str(flower_a) == "Квіти (60.00 см, вік 2 р., 150.00 грн)"
    assert repr(flower_a) == (
        "Flower(species='Квіти', age=2, height=60.0, price=150.0, watering_days=1)"
    )
    assert flower_a == flower_b
    assert flower_a != tree_same_fields
    assert flower_a.__eq__("не рослина") is NotImplemented
    assert flower_a.__lt__("не рослина") is NotImplemented
    assert flower_a < taller_tree
    assert sorted([taller_tree, flower_a]) == [flower_a, taller_tree]


def test_frozen_dataclasses_are_immutable_and_comparable() -> None:
    """Перевіряє незмінність та порівняння @dataclass(frozen=True) CareSummary і ReportSummary."""
    care_a = CareSummary(
        count=2,
        total_care_score=124.0,
        average_care_score=62.0,
        highest_care_species="Квіти",
        highest_care_score=75.0,
    )
    care_b = CareSummary(
        count=2,
        total_care_score=124.0,
        average_care_score=62.0,
        highest_care_species="Квіти",
        highest_care_score=75.0,
    )
    assert care_a == care_b
    assert {care_a, care_b} == {care_a}

    with pytest.raises(dataclasses.FrozenInstanceError):
        care_a.count = 5  # type: ignore[misc]

    report_summary = ReportSummary(
        count=1,
        frequent_watering_count=1,
        average_price=150.0,
        tallest_plant=Flower("Квіти", 2, 60.0, 150.0, 1),
        species=("Квіти",),
        average_price_by_species=(("Квіти", 150.0),),
        care=care_a,
    )
    with pytest.raises(dataclasses.FrozenInstanceError):
        report_summary.average_price = 999.0  # type: ignore[misc]


def test_protocol_and_duck_typing_without_inheritance() -> None:
    """Перевіряє роботу спільних функцій аналізу з будь-яким об'єктом, що підтримує протокол."""

    class CustomBonsai:
        """Незалежний клас без наслідування від Plant, що реалізує протокол CareScored."""

        @property
        def species(self) -> str:
            return "Бонсай"

        @property
        def age(self) -> int:
            return 15

        @property
        def height(self) -> float:
            return 45.0

        @property
        def price(self) -> float:
            return 2500.0

        @property
        def watering_days(self) -> int:
            return 1

        def care_score(self) -> float:
            return 210.0

    items: list[CareScored] = [
        Tree("Хвойні", 4, 120.5, 450.0, 7),
        Flower("Квіти", 2, 60.0, 150.0, 1),
        CustomBonsai(),
    ]

    best = highest_care_plant(items)
    assert best is not None
    assert best.species == "Бонсай"
    assert best.care_score() == 210.0
    assert total_care_score(items) == round(72.54 + 135.0 + 210.0, 2)
    assert average_care_score(items) == round(total_care_score(items) / 3, 2)

    ordered = sort_by_care_score(items)
    assert [item.species for item in ordered] == ["Бонсай", "Квіти", "Хвойні"]

    summary = care_summary(items)
    assert summary.count == 3
    assert summary.highest_care_species == "Бонсай"
    assert summary.highest_care_score == 210.0
