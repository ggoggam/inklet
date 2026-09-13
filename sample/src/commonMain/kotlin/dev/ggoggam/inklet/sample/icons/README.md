# Sample Lucide vectors

Plus, Check, X, Heart and ArrowUpRight are adapted from the Compose Multiplatform
[Lucide vectors in compose-icons](https://github.com/composablehorizons/compose-icons/tree/37d0e9fbe7f162c4b10df1eb302c983d30029454/icons-lucide-cmp)
at commit `37d0e9fbe7f162c4b10df1eb302c983d30029454`.

Changes are limited to the sample package, internal visibility, cache field names
and Kotlin formatting. Path geometry, 24dp viewports and 2-unit rounded strokes
are unchanged. Material `Icon` supplies theme tint; the favorite toggle uses its
native checked container color with the same outline heart.

The upstream ISC and Feather MIT notices are preserved in
`sample/src/commonMain/composeResources/files/lucide-LICENSE.txt`, which is bundled
as a sample resource. No icon library dependency is needed.
