export interface Bot {
    id: string,
    name: string,
    color?: number,
    position?: {
        x: number,
        y: number
    }
}