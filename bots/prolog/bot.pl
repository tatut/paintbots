:- use_module(library(http/http_client)).

url(URL) :- getenv("PAINTBOTS_URL", URL) ; URL='http://localhost:31173'.

post(FormData, Result) :-
     url(URL),
     http_post(URL, form(FormData), Result, []).

% Calculate points for a line from [X1,Y1] to [X2,Y2]
line([X1, Y1], [X2,Y2], Points) :-
    Dx is abs(X1-X2),
    Dy is abs(Y1-Y2),
    (Dx > Dy -> Steps = Dx; Steps = Dy),
    XStep is (X2 - X1) / Steps,
    YStep is (Y2 - Y1) / Steps,
    line_([X1,Y1], [X2,Y2], XStep, YStep, Steps, Points).

% All steps done, return end point
line_(_, [X,Y], _, _, 0, [[X,Y]]).

% More steps to do
line_([Xp,Yp], To, XStep, YStep, Steps, [[X,Y]|Rest]) :-
    X is round(Xp),
    Y is round(Yp),
    Xn is Xp + XStep,
    Yn is Yp + YStep,
    StepsN is Steps - 1,
    line_([Xn,Yn], To, XStep, YStep, StepsN, Rest).

%% Updated bot state from HTTP clients form data
state(Res, bot(_, Xp, Yp, Col)) :-
    member(x=X, Res),
    member(y=Y, Res),
    member(color=Col, Res),
    atom_to_term(X, Xp, []),
    atom_to_term(Y, Yp, []).

%% Register bot with name, fetches and returns initial state of the bot
register(Name, State) :-
    post([register = Name], Id),
    post([id = Id, info = ""], Res),
    State = bot(Id, _,_ ,_),
    state(Res, State).

%% Bot commands take form: command(StateBefore, CommandArgs, StateAfter)
move(bot(Id,_,_,_), Dir, bot(Id, X, Y, C)) :-
    post([id = Id, move = Dir], Res),
    state(Res, bot(Id, X, Y, C)).

paint(bot(Id,_,_,_), bot(Id, X, Y, C)) :-
    post([id = Id, paint = ""], Res),
    state(Res, bot(Id, X, Y, C)).

draw_line(S0, To, S1) :-
    S0=bot(_,X,Y,_),
    line([X,Y], To, Points),
    writeln(line(from([X,Y]),to(To),points(Points))),
    traverse(S0, Points, S1).

% Traverse empty points
traverse(S0, [], S0).

% Traverse 1 point, just paint it
traverse(S0, [_], S1) :-
    paint(S0, S1).

% Two or more points, paint and move
traverse(SIn, [_,P2|Points], SOut) :-
    paint(SIn, S0),
    move_to(S0, P2, S1),
    traverse(S1, [P2|Points], SOut).

% At position, do nothing
move_to(bot(Id,X,Y,C), [X,Y], bot(Id,X,Y,C)).

% Not at position, move horizontally or vertically
move_to(SIn, [Xt,Yt], SOut) :-
    SIn=bot(_,Xp,Yp,_),
    once(dir([Xp,Yp],[Xt,Yt],Dir)),
    move(SIn, Dir, S1),
    move_to(S1, [Xt,Yt], SOut).

bye(bot(Id,_,_,_)) :-
    post([id=Id, bye=""], _Out).

say(bot(Id,_,_,_), Msg, bot(Id,X,Y,C)) :-
    post([id=Id,msg=Msg], Res),
    state(Res, bot(Id,X,Y,C)).

% Determine which direction to go, based on two points
dir([X1,_],[X2,_], "RIGHT") :- X1 < X2.
dir([X1,_],[X2,_], "LEFT") :- X1 > X2.
dir([_,Y1],[_,Y2], "DOWN") :- Y1 < Y2.
dir([_,Y1],[_,Y2], "UP") :- Y1 > Y2.

line_demo(Name) :-
    register(Name, S0),
    draw_line(S0, [80, 50], S1),
    bye(S1).

circle_demo(Name) :-
    register(Name, S0),
    S0 = bot(_, X,Y, _),
    draw_circle(S0, [X, Y], 10, 0, 0.1, S1),
    bye(S1).

draw_circle(S, _, _, Ang, _, S) :-
    Max is 2*pi,
    Ang >= Max.

draw_circle(SIn, [Xc, Yc], R, Ang, AngStep, SOut) :-
    X is round(Xc + R * cos(Ang)),
    Y is round(Yc + R * sin(Ang)),
    move_to(SIn, [X,Y], S1),
    paint(S1, S2),
    AngN is Ang + AngStep,
    draw_circle(S2, [Xc, Yc], R, AngN, AngStep, SOut).

%% Draw the universal peace symbol in the center
peace(Name) :-
    register(Name, S0),
    say(S0, "Peace to the world!", S0),
    move_to(S0, [80, 20], S1),
    draw_line(S1, [80,50], S2),
    draw_line(S2, [57,68], S3),
    move_to(S3, [80,50], S4),
    draw_line(S4, [103,68], S5),
    move_to(S5, [80,80], S6),
    draw_line(S6, [80,50], S7),
    draw_circle(S7, [80, 50], 30, 0, 0.05, S8),
    bye(SFinal).
