//PART PROPERTIES
Property Name(txt);
Property Sequence(txt);

//PART TYPES
Part [Promoter](Name, Sequence);
Part [RBS](Name, Sequence);
Part [Gene](Name, Sequence);
Part [Terminator](Name, Sequence);

[Promoter] a(.Name("a"), .Sequence("AAAAGAGA"));
[Promoter] b(.Name("b"), .Sequence("TTTTGAGA"));
[RBS] c(.Name("c"), .Sequence("GGGGGAGA"));
[Gene] d(.Name("d"), .Sequence("CCCCGAGA"));
[Terminator] e(.Name("e"), .Sequence("AAAAGAGA"));

//GOAL PARTS
Device test1(a, b, c, d, e);

Device[] devices = [test1];
