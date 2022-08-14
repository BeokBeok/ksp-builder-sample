# ksp-builder-sample

## 결과물
<details>

```kotlin
internal class MutablePerson(
	var name: kotlin.String,
	var age: kotlin.Int?,
	var email: kotlin.String?,
	var contact: kotlin.Pair<kotlin.String, kotlin.String, >?,
) {
	fun toPerson(): Person = Person(
		name = name,
		age = age,
		email = email,
		contact = contact,
	)
}
```

```kotlin
class PersonBuilder(
	name: kotlin.String,
) {

	private val mutablePerson: MutablePerson = MutablePerson(
		name = name,
		age = null,
		email = null,
		contact = null,
	)

	fun age(age: kotlin.Int): PersonBuilder {
		mutablePerson.age = age
		return this
	}

	fun email(email: kotlin.String): PersonBuilder {
		mutablePerson.email = email
		return this
	}

	fun contact(contact: kotlin.Pair<kotlin.String, kotlin.String, >): PersonBuilder {
		mutablePerson.contact = contact
		return this
	}

	fun build(): Person = mutablePerson.toPerson()

}
```

</details>

ref. https://github.com/Darvishiyan/KSP-Builder-Sample
